const fs = require('fs');
const path = require('path');
const os = require('os');

const homeDir = os.homedir();
const skillSource = path.join(__dirname, '..', 'SKILL.md');

if (fs.existsSync(skillSource)) {
  const skillPaths = [
    path.join(homeDir, '.claude', 'skills', 'library-insight'),
    path.join(homeDir, '.agents', 'skills', 'library-insight'),
    path.join(homeDir, '.codex', 'skills', 'library-insight'),
    path.join(homeDir, '.cursor', 'skills', 'library-insight'),
    path.join(homeDir, '.gemini', 'skills', 'library-insight'),
    path.join(homeDir, '.gemini', 'config', 'skills', 'library-insight'),
    path.join(homeDir, '.copilot', 'skills', 'library-insight'),
    path.join(homeDir, '.junie', 'skills', 'library-insight')
  ];

  skillPaths.forEach((destDir) => {
    try {
      fs.mkdirSync(destDir, { recursive: true });
      fs.copyFileSync(skillSource, path.join(destDir, 'SKILL.md'));
      console.log(`[Library Insight] Registered AI Agent Skill at: ${destDir}`);
    } catch (e) {
      // Ignore if directory permissions or access fail
    }
  });
}
