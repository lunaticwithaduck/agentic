---
name: cli-builder
description: Design and build CLI tools with proper argument parsing, help text, and POSIX conventions
activation:
  keywords: ["cli", "command line", "cli tool", "cli builder", "command line interface", "argument parsing"]
  file_patterns: ["**/cli.*", "**/cli/**", "**/bin/**", "**/cmd/**"]
---

# CLI Tool Design

## Purpose
Design and implement well-structured command-line tools that follow
POSIX conventions with proper argument parsing, help text, and error handling.

## Instructions

1. **Define Commands and Subcommands**
   - Identify the primary actions the tool performs
   - Group related actions under subcommands (e.g., `tool user create`)
   - Keep command names short, descriptive, and verb-based
   - Limit nesting to 2 levels (command + subcommand)
   - Include implicit commands: help, version, completion

2. **Parse Arguments**
   - **Required arguments**: positional, in a logical order
   - **Optional arguments**: use flags with short (`-v`) and long (`--verbose`) forms
   - **Boolean flags**: `--flag` to enable, `--no-flag` to disable
   - **Value flags**: `--output file.txt` or `--output=file.txt`
   - **Repeated flags**: `--include foo --include bar` or `-vvv` for verbosity
   - Accept stdin with `-` as filename convention

3. **Implement Help Text**
   - Auto-generate from command definitions
   - Structure:
     ```
     Usage: tool <command> [options] [arguments]

     Description of what the tool does.

     Commands:
       create    Create a new resource
       list      List all resources

     Options:
       -o, --output <file>  Output file (default: stdout)
       -v, --verbose        Increase verbosity
       -h, --help           Show this help
       --version            Show version

     Examples:
       tool create --name "My Project"
       tool list --format json
     ```
   - Include 2-3 practical examples in help text

4. **Add Input Validation**
   - Validate required arguments are present
   - Check argument types and ranges
   - Validate file paths exist (for input files)
   - Validate enum values against allowed options
   - Provide clear error messages with usage hints on validation failure

5. **Handle Errors Gracefully**
   - Exit code 0 for success, 1 for general errors, 2 for usage errors
   - Print errors to stderr, output to stdout
   - Include the failing input in error messages
   - Suggest corrections for common mistakes (did you mean...?)
   - Never print stack traces unless --debug is set

6. **Support Configuration Files**
   - Look for config in standard locations (`.toolrc`, `tool.config.json`)
   - Precedence: CLI flags > env vars > config file > defaults
   - Support `--config` flag for custom config path
   - Document all configuration options

7. **Shell Completion**
   - Generate completion scripts for bash, zsh, fish
   - Complete subcommands, flags, and known values
   - Provide installation instructions for each shell

8. **Follow POSIX Conventions**
   - Use `--` to separate flags from positional arguments
   - Support `-` for stdin/stdout
   - Respect `NO_COLOR` environment variable
   - Support piping (detect TTY for formatting decisions)
   - Use exit codes consistently

## Output Format

Provide:
1. Command structure diagram
2. Full argument/flag definitions with types and defaults
3. Implementation code with argument parsing
4. Help text output
5. Error handling examples
6. Shell completion script (for primary shell)
