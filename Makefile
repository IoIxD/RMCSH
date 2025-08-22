.PHONY: all

VERSIONS = a1.1.2 a1.2.3_01-0958 a1.2.5 a1.2.6 b1.1_01 b1.2 b1.3_01 b1.4_01 b1.5_01 b1.6 b1.6.5 b1.6.6 b1.7 b1.8 b1.8.1 b1.9-pre5 1.0.0 1.1 1.2.3 1.2.5

all: patch dist

env_setup: mcp_dir_setup retromcp_download
setup: env_setup $(foreach version,$(VERSIONS),setup_$(version))
decompile: setup $(foreach version,$(VERSIONS),decompile_$(version))
patch: decompile $(foreach version,$(VERSIONS),patch_$(version))
dist: natives $(foreach version,$(VERSIONS),mod_dist_$(version))

dist_dir_setup:
ifeq ("$(wildcard ./.dist)","")
	@echo "$(wildcard ./.dist/)"
	@echo "Making .dist directory"
	@mkdir .dist
endif

mcp_dir_setup:
ifeq ("$(wildcard ./.mcp)","")
	@echo "$(wildcard ./.mcp/)"
	@echo "Making .mcp directory"
	@mkdir .mcp
endif

retromcp_download:
ifeq ("$(wildcard ./.mcp/RetroMCP-Java-CLI.jar)","")
	@echo "Downloading RetroMCP"
	wget https://github.com/MCPHackers/RetroMCP-Java/releases/download/v1.0/RetroMCP-Java-CLI.jar
	mv RetroMCP-Java-CLI.jar ./.mcp/RetroMCP-Java-CLI.jar
endif

setup_%:
# idk there's supposed to be way built into make to test if a directory exists, but for me at least, empty comparisons end up succeeding. so we just use bash for this part.
	@echo setting up $*
	@if [ -d "./.mcp/$*" ]; then \
		echo "./.mcp/$*" exists, skipping setup; \
	else \
		mkdir ./.mcp/$*; cd ./.mcp/$*; java -jar ../RetroMCP-Java-CLI.jar setup $*; \
	fi

decompile_%:
	@echo decompiling $*
	@if [ ! -d "./.mcp/$*" ]; then \
		echo "./.mcp/$*" does not exist, cannot decompile; \
	else \
		if [ -d "./.mcp/$*/minecraft/src" ]; then \
			echo "./.mcp/$*/minecraft/src" exists, skipping decompile; \
		else \
			cd ./.mcp/$*; java -jar ../RetroMCP-Java-CLI.jar decompile $*; \
		fi \
	fi

redecompile_%:
	@echo \(re\)decompiling $*
	@if [ ! -d "./.mcp/$*" ]; then \
		echo "./.mcp/$*" does not exist, cannot decompile; \
	else \
		cd ./.mcp/$*; java -jar ../RetroMCP-Java-CLI.jar decompile $*; \
	fi

recompile_%:
	@echo \(re\)decompiling $*
	@if [ ! -d "./.mcp/$*" ]; then \
		echo "./.mcp/$*" does not exist, cannot recompile; \
	else \
		cd ./.mcp/$*; java -jar ../RetroMCP-Java-CLI.jar recompile $*; \
	fi

patch_%:
	@echo patching $*
	@if [ ! -d "./.mcp/$*" ]; then \
		echo "./.mcp/$*" does not exist, cannot patch; \
	else \
		if [ ! -d "./.mcp/$*/minecraft/src/net" ]; then \
			echo "./.mcp/$*/minecraft/src/net" does not exist, cannot patch; \
		else \
			rm -rf ./.mcp/$*/minecraft/src/net; \
			cp -r ./.mcp/$*/minecraft/source/net ./.mcp/$*/minecraft/src/net; \
			cp ./patches/RMCSHNative.java ./.mcp/$*/minecraft/src/net/minecraft/src; \
			cp ./patches/$*.patch ./.mcp/$*/minecraft/src/net; \
			cd ./.mcp/$*/minecraft/src/net; echo ">$*.patch"; git apply $*.patch; rm $*.patch; \
		fi \
	fi

test_%: recompile_%
	@echo testing $*
	@if [ ! -d "./.mcp/$*" ]; then \
		echo "./.mcp/$*" does not exist, cannot decompile; \
	else \
		cd ./.mcp/$*; java -jar ../RetroMCP-Java-CLI.jar start client $*; \
	fi

mod_dist_%:
	@echo packaging $*
	@if [ ! -d "./.mcp/$*" ]; then \
		echo "./.mcp/$*" does not exist, cannot cannot package dll; \
	else \
		if [ ! -d "./.mcp/$*/minecraft/src" ]; then \
			echo "./.mcp/$*/minecraft/src" does not exist, cannot continue; \
		else \
			cp ./.dist/RMCSHNative.dll ./.mcp/$*/minecraft/src; \
			cp ./.dist/libRMCSHNative.so ./.mcp/$*/minecraft/src; \
			cd ./.mcp/$*; \
				java -jar ../RetroMCP-Java-CLI.jar recompile $*; \
				java -jar ../RetroMCP-Java-CLI.jar reobfuscate $*; \
				java -jar ../RetroMCP-Java-CLI.jar build $*; \
			cd ../..; \
			cp ./.mcp/$*/build/minecraft.zip ./.dist/RMCSH-$*.zip; \
		fi \
	fi

natives: dist_dir_setup native_linux native_windows

native_linux:
	gcc -c -fPIC -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux src/native.c
	gcc -nostdlib -lgcc native.o -shared -o ./.dist/libRMCSHNative.so

native_windows:
	x86_64-w64-mingw32-gcc -c -fPIC -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux src/native.c
	x86_64-w64-mingw32-gcc native.o -shared -o ./.dist/RMCSHNative.dll

clean:
	rm -rf .mcp
	rm -rf .dist