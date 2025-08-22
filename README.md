# RMCSH

**R**etro **M**ine**c**raft **S**peedrun **H**elper is a mod spanning from Alpha 1.1.2 to Release 1.2.5 that does two things:

a.) Use native libraries to present information from the game in such a way that programs like AutoSplitters can easily read them and properly split.

b.) Display the coordinates on screen at all time as some older versions don't have this information in the F3 menu

<img width="888" height="421" alt="image" src="https://github.com/user-attachments/assets/2c5941e4-9fb0-4d31-8a0c-4320247f9a55" />

Its primary use case is allowing older Minecraft Adventure maps to be speedrun convienently, as well as allow certain maps to start based on where the player coordinates are. The displaying of coordinates most useful for older versions where the F3 debug menu doesn't show coordinates, but its here for all versions so that you don't have to submit a run with the F3 menu open.

# Building

The repo uses a Makefile to setup the environment (it uses [RetroMCP-Java](https://github.com/MCPHackers/RetroMCP-Java)), decompile all the versions of Minecraft that are supported, patch them, get them ready for distribution, and build/package the native DLL.

The make commands that can be used are:

```sh
make # Runs all of the below commands in the correct order
make env_setup # Setup the environment
make setup # Setup folders for all the supported versions of the game
make decompile # Decompile all the supported versions of the game
make patch # Patch all the supported versions of the game
```