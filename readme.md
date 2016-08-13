# NWScriptBuilder
Build utility for nss files.

## Objective
The idea for this project was to create a tool which would make it easier to 
integrate Skywing's Advanced NwnScript compiler with any text editor or IDE.

Currently, this manages to detect file changes and tell the compiler to 
recompile. It also is able to notice if the file is an include, and then 
proceeds to find the files which depend on that include, all the way until it 
finds the mains and StartingConditional files which depended on that modified 
file, to then compile those specific files.

## Commands
* `watch <directory>` will restart the watcher and will check for changes and 
recompile the files if needed. Argument may be quoted if there are spaces in the 
path given.
* `clear` will remove all watches to directories that currently exist.
* `exit` will end all watches and exit the application.
* `all` will compile the entire contents of directories which are under watch.
* `chars` will display the count for the first character in the watched 
directory. This is just a facility for setting up the multi-processs 
compilation.
* `chars-recommend <processes>` goes even further than the `chars` command by 
recommending a specific combination to use for splitting the load across 
multiple processes. Result is automatically sent to the clipboard so you can 
just paste it in your configuration file.

## Requirements
This application requires Skywing's Advanced Script Compiler, which can be found
here:

http://neverwintervault.org/project/nwn2/other/tool/advanced-script-compiler-nwn2

## Setup
All you really need to do is configure using the provided application.conf file. 
Details can be found inside that file. After that, simply run the application 
through the command shell. 

It is strongly recommended that you extract all game scripts into a directory 
and include it for compilation instead of telling the compiler to load all game 
files. Doing so will speed up compilation time as the compiler won't have to 
unzip the script files in the data folder every time you modify a file.

## Note On Parallelization
The partitioning used for parallelizing the compilation is very simple. It uses
the first letter of the file being compiled to determine what thread it should
be sent to.

The utility only tries to parallelize when running a batch compile. When
compiling in watch mode it doesn't parallelize as this probably won't make
a difference.

