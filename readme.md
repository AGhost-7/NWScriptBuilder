# Objective
The idea for this project was to create a tool which would make it easier to 
integrate Skywing's Advanced NwnScript compiler with any text editor or IDE.

Currently, this manages to detect file changes and tell the compiler to recompile. It
also is able to notice if the file is an include, and then proceeds to find the files
which depend on that include, all the way until it finds the mains and 
StartingConditional files which depended on that modified file, to then compile those
specific files.

# Commands
* `watch <directory>` will restart the watcher and will check for changes and 
recompile the files if needed. Argument may be quoted if there are spaces in the 
path given.
* `clear` will remove all watches to directories that currently exist.
* `exit` will end all watches and exit the application.
* `all` will compile the entire contents of directories which are under watch.
* `chars` will display the count for the first character in the watched directory.
This is just a facility for setting up the multi-processs compilation.
* `chars-recommend <processes>` goes even further than the `chars` command by 
recommending a specific combination to use for splitting the load across multiple
processes. Result is automatically sent to the clipboard so you can just paste it in
your configuration file.

# Setup
All you really need to do is configure using the provided application.conf file. 
Details can be found inside that file. After that, simply run the application through
the command shell.