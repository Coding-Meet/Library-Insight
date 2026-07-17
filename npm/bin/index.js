#!/usr/bin/env node

const { spawn, execSync } = require('child_process');
const path = require('path');

// 1. Verify Java is installed on user's system
try {
  execSync('java -version', { stdio: 'ignore' });
} catch (e) {
  console.error('\nError: Java is not installed or not found on your system PATH.');
  console.error('Library Insight requires Java 17 or higher to run.');
  console.error('Please install a JDK (e.g. from https://adoptium.net) and try again.\n');
  process.exit(1);
}

// 2. Resolve absolute path to the lib/ directory containing all dependency JARs
const libPath = path.join(__dirname, '..', 'lib', '*');
const mainClass = 'com.meet.libraryinsight.cli.MainKt';

// 3. Spawn Java process directly forwarding classpath and CLI arguments
const args = ['-cp', libPath, mainClass, ...process.argv.slice(2)];
const child = spawn('java', args, { stdio: 'inherit' });

child.on('close', (code) => {
  process.exit(code);
});
