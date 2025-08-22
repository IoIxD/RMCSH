VERSIONS=("a1.1.2")

all: mcp_dir_setup retromcp_download

mcp_dir_setup:
echo "$(wildcard ./mcp)"
ifeq ("$(wildcard ./mcp)","")
	@echo "Making .mcp directory"
	@mkdir .mcp
endif

retromcp_download:
ifeq ("$(wildcard .mcp/RetroMCP-Java-CLI.jar)","")
	@echo "Downloading RetroMCP"
	wget https://github.com/MCPHackers/RetroMCP-Java/releases/download/v1.0/RetroMCP-Java-CLI.jar
	mv RetroMCP-Java-CLI.jar ./.mcp/RetroMCP-Java-CLI.jar
endif

clean:
	rm -rf .mcp