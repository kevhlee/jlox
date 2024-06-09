# jLox 🐟

Implementations of the [Lox](https://craftinginterpreters.com) programming language written in Java.

This project contains both the tree-walk (AST) and virtual machine implementations of Lox.

## Setup

__Requirements:__

- Java 17+

To build the project, run `./gradlew buildLox`. This will build and create executable JARs containing the Lox
interpreter implementations in the `bin` directory:

```log
bin
├── interpreter-ast.jar   // contains the tree-walk implementation
└── interpreter-vm.jar    // contains the virtual machine implementation
```