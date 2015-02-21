The idea for this project was to create a tool which would make it easier to 
integrate Skywing's Advanced NwnScript compiler with any text editor or IDE.

Currently, this manages to detect file changes and tell the compiler to recompile. It
also is able to notice if the file is an include, and then proceeds to find the 
files which depend on that include, all the way until it finds the mains and
StartingConditional files which depended on that modified file.