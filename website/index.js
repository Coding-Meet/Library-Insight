// Interactive Tabs switching logic
function switchTab(evt, tabId) {
    // Hide all tab panes
    const tabPanes = document.getElementsByClassName("tab-pane");
    for (let i = 0; i < tabPanes.length; i++) {
        tabPanes[i].classList.remove("active");
    }

    // Remove active class from all tab buttons
    const tabBtns = document.getElementsByClassName("tab-btn");
    for (let i = 0; i < tabBtns.length; i++) {
        tabBtns[i].classList.remove("active");
    }

    // Show current tab pane
    document.getElementById(tabId).classList.add("active");
    evt.currentTarget.classList.add("active");
}

// Terminal typing simulation logic
const terminalLines = [
    { type: 'input', text: 'library-insight scan sample.jar --sources sample-sources.jar' },
    { type: 'output', text: '-> Scanning jar bytecode...\n-> Parsing Kotlin metadata declarations...\n-> Matching comments from sources...\nScan complete! Found 5 classes across package com.meet.sample.\nSaved API index to: build/library-insight-index.json\n' },
    { type: 'input', text: 'library-insight search Calculator' },
    { type: 'output', text: 'Matches found:\n  - Class: com.meet.sample.Calculator (com.meet.sample)\n' },
    { type: 'input', text: 'library-insight explain Calculator' },
    { type: 'output', text: 'Class:       com.meet.sample.Calculator\nPackage:     com.meet.sample\nVisibility:  public\nSupertypes:  com.meet.sample.Greeter\n\nMethods:\n  - public fun greet(name: String): String\n    // Implementation of Greeter.greet returning a friendly greeting.' }
];

async function runTerminalSimulation() {
    const terminal = document.getElementById("terminal-body");
    if (!terminal) return;
    
    terminal.innerHTML = "";

    for (const line of terminalLines) {
        if (line.type === 'input') {
            // Type the prompt line char-by-char
            const promptSpan = document.createElement("span");
            promptSpan.className = "prompt";
            promptSpan.textContent = "$ ";
            terminal.appendChild(promptSpan);

            const commandSpan = document.createElement("span");
            terminal.appendChild(commandSpan);

            for (let i = 0; i < line.text.length; i++) {
                commandSpan.textContent += line.text[i];
                await new Promise(resolve => setTimeout(resolve, 50));
            }
            
            terminal.appendChild(document.createElement("br"));
            await new Promise(resolve => setTimeout(resolve, 400));
        } else {
            // Output is printed immediately with a small line delay
            const outputSpan = document.createElement("span");
            outputSpan.className = "output";
            terminal.appendChild(outputSpan);

            const textLines = line.text.split("\n");
            for (const textLine of textLines) {
                outputSpan.textContent += textLine + "\n";
                terminal.scrollTop = terminal.scrollHeight;
                await new Promise(resolve => setTimeout(resolve, 80));
            }
            await new Promise(resolve => setTimeout(resolve, 800));
        }
    }
}

// Start simulation on load
document.addEventListener("DOMContentLoaded", () => {
    runTerminalSimulation();
    // Restart typing animation every 18 seconds
    setInterval(runTerminalSimulation, 18000);
});
