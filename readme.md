# What? What is this?
The idea for this project was to create a tool which would make it easier to 
integrate Skywing's Advanced NwnScript compiler with any text editor or IDE.

Currently, this manages to detect file changes and tell the compiler to recompile. It
also is able to notice if the file is an include, and then proceeds to find the 
files which depend on that include, all the way until it finds the mains and
StartingConditional files which depended on that modified file.

# WIP features
* Parallel compilation
* Autocomplete file generation by reading nss file prototypes.

# Commands
* `watch` will spawn a thread which will check the directory for any file changes and
recompile the files if needed.
* `clear` will remove all watches to directories that currently exist.
* `exit` will end all watchers and exit the application.

You can also point to a configuration file at startup. For example, using 
`NWScriptBuilder project2.conf`.
