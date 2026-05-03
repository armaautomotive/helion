package helion;

public final class HelionWebUi {
    private HelionWebUi() {
    }

    public static String indexHtml() {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Helion</title>
                  <style>
                    :root {
                      --bg: #f4efe6;
                      --panel: #fffaf2;
                      --panel-strong: #fffdf8;
                      --ink: #182022;
                      --muted: #5d6a6f;
                      --line: #d6cbb7;
                      --accent: #0f6f78;
                      --accent-2: #c96d31;
                      --accent-soft: #edf8f8;
                    }
                    * { box-sizing: border-box; }
                    body {
                      margin: 0;
                      font-family: Georgia, "Iowan Old Style", serif;
                      color: var(--ink);
                      background:
                        radial-gradient(circle at top left, rgba(201,109,49,.12), transparent 28rem),
                        radial-gradient(circle at bottom right, rgba(15,111,120,.12), transparent 28rem),
                        var(--bg);
                    }
                    .app {
                      display: grid;
                      grid-template-columns: 17rem 1fr;
                      min-height: 100vh;
                      gap: 1rem;
                      padding: 1rem;
                    }
                    .panel {
                      background: rgba(255,250,242,.92);
                      border: 1px solid var(--line);
                      border-radius: 1.2rem;
                      padding: 1rem;
                      box-shadow: 0 1rem 2rem rgba(24,32,34,.08);
                      backdrop-filter: blur(6px);
                    }
                    .workspace {
                      display: grid;
                      grid-template-rows: auto auto 1fr;
                      gap: 1rem;
                    }
                    h1, h2, h3 { margin: 0 0 .75rem; }
                    h1 { font-size: 1.6rem; }
                    h2 { font-size: 1rem; text-transform: uppercase; letter-spacing: .08em; color: var(--muted); }
                    .brand { margin-bottom: 1rem; }
                    .brand p { color: var(--muted); margin: .3rem 0 0; }
                    .agents button, .tab-button, .mini, .send {
                      border: 1px solid var(--line);
                      border-radius: .8rem;
                      background: white;
                      padding: .7rem .85rem;
                      cursor: pointer;
                      font: inherit;
                    }
                    .agents button {
                      width: 100%;
                      text-align: left;
                      margin-bottom: .5rem;
                    }
                    .agent-card {
                      display: grid;
                      gap: .35rem;
                    }
                    .agent-topline {
                      display: flex;
                      justify-content: space-between;
                      align-items: center;
                      gap: .5rem;
                    }
                    .agent-meta {
                      display: flex;
                      flex-wrap: wrap;
                      gap: .35rem;
                    }
                    .badge {
                      display: inline-flex;
                      align-items: center;
                      border-radius: 999px;
                      padding: .15rem .5rem;
                      font-size: .78rem;
                      line-height: 1.2;
                      border: 1px solid var(--line);
                      background: rgba(255,255,255,.8);
                    }
                    .badge.exec-cloud_running {
                      background: #e9f3ff;
                      border-color: #8ab3e6;
                      color: #1d4f85;
                    }
                    .badge.exec-local_running {
                      background: #edf8f1;
                      border-color: #8dc5a0;
                      color: #1e6b39;
                    }
                    .badge.exec-paused, .badge.runtime-idle {
                      background: #f6f0e5;
                      border-color: #cfb98e;
                      color: #7d5b18;
                    }
                    .badge.exec-disabled {
                      background: #f1eceb;
                      border-color: #c5b0ac;
                      color: #765650;
                    }
                    .badge.runtime-running {
                      background: #e8f8f6;
                      border-color: #7cbeb6;
                      color: #0f6f78;
                    }
                    .badge.runtime-error {
                      background: #fff0ef;
                      border-color: #dd8d87;
                      color: #a1362c;
                    }
                    .agent-subtext {
                      font-size: .8rem;
                      color: var(--muted);
                    }
                    .agents button.active, .tab-button.active {
                      border-color: var(--accent);
                      background: var(--accent-soft);
                    }
                    .muted { color: var(--muted); }
                    .toolbar {
                      display: grid;
                      grid-template-columns: 1fr auto;
                      gap: .75rem;
                      align-items: center;
                    }
                    .status-bar {
                      border: 1px solid var(--line);
                      border-radius: .9rem;
                      padding: .9rem;
                      background: rgba(255,255,255,.7);
                      white-space: pre-wrap;
                    }
                    .tabs {
                      display: flex;
                      flex-wrap: wrap;
                      gap: .5rem;
                    }
                    .tab-panel {
                      display: none;
                      min-height: 0;
                    }
                    .tab-panel.active {
                      display: grid;
                      gap: 1rem;
                      min-height: 0;
                    }
                    .chat-panel {
                      grid-template-rows: 1fr auto;
                    }
                    .response-box, .company-box, .detail-box, .editor-box, .usage-box {
                      border: 1px solid var(--line);
                      border-radius: .9rem;
                      padding: .9rem;
                      background: rgba(255,255,255,.72);
                      white-space: pre-wrap;
                    }
                    .response-box {
                      min-height: 16rem;
                    }
                    .action-block {
                      display: grid;
                      gap: .75rem;
                    }
                    select, textarea, input {
                      width: 100%;
                      border: 1px solid var(--line);
                      border-radius: .8rem;
                      padding: .75rem;
                      font: inherit;
                      background: white;
                    }
                    textarea {
                      min-height: 10rem;
                      resize: vertical;
                    }
                    .send {
                      background: var(--accent);
                      color: white;
                      border: none;
                    }
                    .flash { color: var(--accent-2); min-height: 1.2rem; }
                    .company-actions, .distilled-toolbar {
                      display: grid;
                      grid-template-columns: 1fr auto;
                      gap: .5rem;
                    }
                    .source-item, .distilled-item {
                      display: grid;
                      grid-template-columns: 1fr auto auto;
                      gap: .5rem;
                      align-items: center;
                      padding: .5rem 0;
                      border-bottom: 1px solid var(--line);
                    }
                    .distilled-item {
                      grid-template-columns: 1fr auto;
                    }
                    .source-item:last-child, .distilled-item:last-child { border-bottom: 0; }
                    .content-grid {
                      display: grid;
                      grid-template-columns: minmax(14rem, 18rem) 1fr;
                      gap: 1rem;
                      min-height: 26rem;
                    }
                    .stack {
                      display: grid;
                      gap: 1rem;
                    }
                    .file-list {
                      background: rgba(255,255,255,.5);
                      border: 1px solid var(--line);
                      border-radius: .9rem;
                      padding: .75rem;
                    }
                    .file-list button {
                      width: 100%;
                      text-align: left;
                      margin-bottom: .45rem;
                    }
                    .file-list button:last-child {
                      margin-bottom: 0;
                    }
                    .editor-meta {
                      display: flex;
                      justify-content: space-between;
                      align-items: center;
                      gap: .75rem;
                    }
                    .editor-meta-wrap {
                      display: grid;
                      gap: .75rem;
                    }
                    .editor-title {
                      display: flex;
                      justify-content: space-between;
                      align-items: center;
                      gap: .75rem;
                    }
                    .editor-box textarea {
                      min-height: 24rem;
                    }
                    .role-grid {
                      display: grid;
                      grid-template-columns: 1.35fr .9fr;
                      gap: 1rem;
                    }
                    .role-grid .editor-box textarea {
                      min-height: 22rem;
                    }
                    .role-grid .editor-box.compact textarea {
                      min-height: 10rem;
                    }
                    .usage-list {
                      display: grid;
                      gap: .75rem;
                    }
                    @media (max-width: 1100px) {
                      .app { grid-template-columns: 1fr; }
                      .content-grid { grid-template-columns: 1fr; }
                      .role-grid { grid-template-columns: 1fr; }
                    }
                  </style>
                </head>
                <body>
                  <div class="app">
                    <aside class="panel">
                      <div class="brand">
                        <h1>Helion</h1>
                        <p>Local multi-agent business console</p>
                      </div>
                      <h2>Agents</h2>
                      <div class="agents" id="agents"></div>
                    </aside>

                    <main class="panel workspace">
                      <div class="toolbar">
                        <div>
                          <h2>Session</h2>
                          <div class="muted" id="sessionLabel">No agent selected</div>
                        </div>
                        <select id="mode">
                          <option value="general">general</option>
                          <option value="analyze">analyze</option>
                          <option value="plan">plan</option>
                          <option value="email">email</option>
                        </select>
                      </div>

                      <div class="status-bar" id="agentStatus">Select an agent.</div>

                      <section class="stack">
                        <div class="tabs">
                          <button class="tab-button active" data-tab="chat">Chat</button>
                          <button class="tab-button" data-tab="settings">Settings</button>
                          <button class="tab-button" data-tab="role">Role</button>
                          <button class="tab-button" data-tab="distilled">Distilled</button>
                          <button class="tab-button" data-tab="company">Company</button>
                          <button class="tab-button" data-tab="usage">Usage</button>
                        </div>

                        <div class="tab-panel chat-panel active" data-panel="chat">
                          <div class="response-box" id="responseBox">Response output will appear here.</div>
                          <div class="action-block">
                            <textarea id="prompt" placeholder="Ask the selected agent to do work..."></textarea>
                            <div class="toolbar">
                              <div class="flash" id="flash"></div>
                              <button class="send" id="send">Send</button>
                            </div>
                          </div>
                        </div>

                        <div class="tab-panel" data-panel="settings">
                          <div class="role-grid">
                            <div class="editor-box compact">
                              <div class="editor-title">
                                <strong>Execution State</strong>
                                <button class="mini" id="saveSettings">Save</button>
                              </div>
                              <select id="executionStateSelect">
                                <option value="cloud_running">cloud running</option>
                                <option value="local_running">local running</option>
                                <option value="paused">paused</option>
                                <option value="disabled">disabled</option>
                              </select>
                            </div>
                            <div class="editor-box compact">
                              <div class="editor-title">
                                <strong>Run Interval Seconds</strong>
                              </div>
                              <input id="runIntervalInput" type="number" min="10" step="10" placeholder="300">
                            </div>
                          </div>
                          <div class="detail-box" id="settingsHelp">Agent settings control whether autonomous work runs and how often the supervisor checks this agent.</div>
                        </div>

                        <div class="tab-panel" data-panel="role">
                          <div class="role-grid">
                            <div class="editor-box">
                              <div class="editor-title">
                                <strong>Role</strong>
                                <button class="mini" id="saveRole">Save</button>
                              </div>
                              <textarea id="roleEditor" placeholder="Select an agent to edit its role."></textarea>
                            </div>
                            <div class="stack">
                              <div class="editor-box compact">
                                <div class="editor-title">
                                  <strong>Status</strong>
                                  <button class="mini" id="saveStatus">Save</button>
                                </div>
                                <textarea id="statusEditor" placeholder="Current mission, focus, or state."></textarea>
                              </div>
                              <div class="detail-box" id="roleHelp">Role defines the agent job, boundaries, and expected outputs.</div>
                            </div>
                          </div>
                        </div>

                        <div class="tab-panel" data-panel="distilled">
                          <div class="content-grid">
                            <div class="file-list" id="distilledFiles">Select an agent to load distilled files.</div>
                            <div class="stack">
                              <div class="editor-box">
                                <div class="editor-title">
                                  <strong>Distill Instructions</strong>
                                  <button class="mini" id="saveDistill">Save</button>
                                </div>
                                <textarea id="distillEditor" placeholder="Describe what this agent should extract from company data."></textarea>
                              </div>
                              <div class="detail-box" id="distilledDetails">Select an agent to view workspace context.</div>
                              <div class="editor-box">
                                <div class="editor-meta">
                                  <strong id="distilledFileName">No file selected</strong>
                                  <button class="mini" id="saveDistilled">Save</button>
                                </div>
                                <textarea id="distilledEditor" placeholder="Select a distilled file to edit it here."></textarea>
                              </div>
                            </div>
                          </div>
                        </div>

                        <div class="tab-panel" data-panel="company">
                          <div class="company-box" id="companySources">Loading...</div>
                          <div class="company-actions">
                            <input id="companyPath" placeholder="/path/to/company/folder">
                            <button class="mini" id="addSource">Add</button>
                          </div>
                        </div>

                        <div class="tab-panel" data-panel="usage">
                          <div class="usage-box usage-list" id="usageBox">Loading...</div>
                        </div>
                      </section>
                    </main>
                  </div>
                  <script>
                    let currentAgent = "";
                    let currentDistilledFile = "";
                    let currentAgentDetails = null;

                    async function api(path, options = {}) {
                      const response = await fetch(path, {
                        headers: { "Content-Type": "application/json" },
                        ...options
                      });
                      const data = await response.json();
                      if (!response.ok) {
                        throw new Error(data.error || "Request failed");
                      }
                      return data;
                    }

                    function setFlash(message) {
                      document.getElementById("flash").textContent = message || "";
                    }

                    function renderAgentDetails() {
                      const roleEditor = document.getElementById("roleEditor");
                      const statusEditor = document.getElementById("statusEditor");
                      const distillEditor = document.getElementById("distillEditor");
                      const executionStateSelect = document.getElementById("executionStateSelect");
                      const runIntervalInput = document.getElementById("runIntervalInput");
                      const settingsHelp = document.getElementById("settingsHelp");
                      const roleHelp = document.getElementById("roleHelp");
                      const distilledRoot = document.getElementById("distilledDetails");
                      if (!currentAgentDetails) {
                        roleEditor.value = "";
                        statusEditor.value = "";
                        distillEditor.value = "";
                        executionStateSelect.value = "local_running";
                        runIntervalInput.value = "";
                        settingsHelp.textContent = "Agent settings control whether autonomous work runs and how often the supervisor checks this agent.";
                        roleHelp.textContent = "Role defines the agent job, boundaries, and expected outputs.";
                        distilledRoot.textContent = "Select an agent to view workspace context.";
                        return;
                      }
                      roleEditor.value = currentAgentDetails.role || "";
                      statusEditor.value = currentAgentDetails.status || "";
                      distillEditor.value = currentAgentDetails.distill || "";
                      executionStateSelect.value = currentAgentDetails.executionState || "local_running";
                      runIntervalInput.value = readRunInterval(currentAgentDetails.status || "") || "";
                      settingsHelp.textContent =
                        "Current execution state: " + (currentAgentDetails.executionState || "local_running") +
                        "\\nCurrent interval: " + (runIntervalInput.value || "300") + " seconds";
                      roleHelp.textContent =
                        "Agent: " + currentAgent +
                        "\\n\\nRole controls what work this agent should do and how it should behave.";
                      distilledRoot.textContent =
                        "Runtime:\\n" + (currentAgentDetails.runtime || "") +
                        "\\n\\nWorkspace:\\n" + (currentAgentDetails.workspace || "");
                    }

                    function activateTab(tabName) {
                      document.querySelectorAll(".tab-button").forEach(button => {
                        button.classList.toggle("active", button.dataset.tab === tabName);
                      });
                      document.querySelectorAll(".tab-panel").forEach(panel => {
                        panel.classList.toggle("active", panel.dataset.panel === tabName);
                      });
                    }

                    async function loadAgents() {
                      const data = await api("/api/agents");
                      const root = document.getElementById("agents");
                      root.innerHTML = "";
                      data.agents.forEach(agent => {
                        const button = document.createElement("button");
                        button.className = agent.id === currentAgent ? "active" : "";
                        button.onclick = () => selectAgent(agent.id);
                        button.innerHTML =
                          "<div class='agent-card'>" +
                          "<div class='agent-topline'><strong>" + agent.id + "</strong><span class='badge exec-" + agent.executionState + "'>" + agent.executionState.replaceAll("_", " ") + "</span></div>" +
                          "<div class='agent-meta'>" +
                          "<span class='badge runtime-" + agent.runtimeState + "'>" + agent.runtimeState + "</span>" +
                          "<span class='badge'>successes " + agent.totalSuccesses + "</span>" +
                          (agent.consecutiveFailures > 0 ? "<span class='badge runtime-error'>failures " + agent.consecutiveFailures + "</span>" : "") +
                          "</div>" +
                          (agent.lastErrorMessage ? "<div class='agent-subtext'>" + escapeHtml(agent.lastErrorMessage) + "</div>" : "") +
                          "</div>";
                        root.appendChild(button);
                      });
                      if (!currentAgent && data.agents.length) {
                        await selectAgent(data.agents[0].id);
                      }
                    }

                    function readRunInterval(statusText) {
                      const lines = (statusText || "").split("\\n");
                      for (const line of lines) {
                        const lower = line.toLowerCase();
                        if (lower.startsWith("run interval seconds:")) {
                          return line.substring(line.indexOf(":") + 1).trim();
                        }
                      }
                      return "";
                    }

                    async function selectAgent(agentId) {
                      currentAgent = agentId;
                      await loadAgentButtons();
                      const details = await api("/api/agent?id=" + encodeURIComponent(agentId));
                      currentAgentDetails = details;
                      document.getElementById("sessionLabel").textContent = "Agent: " + agentId;
                      document.getElementById("agentStatus").textContent = details.status || "No status.";
                      renderAgentDetails();
                      await loadDistilledFiles();
                    }

                    async function loadAgentButtons() {
                      const root = document.getElementById("agents");
                      root.querySelectorAll("button").forEach(button => {
                        const name = button.querySelector("strong");
                        button.classList.toggle("active", name && name.textContent === currentAgent);
                      });
                    }

                    function escapeHtml(value) {
                      return (value || "")
                        .replaceAll("&", "&amp;")
                        .replaceAll("<", "&lt;")
                        .replaceAll(">", "&gt;");
                    }

                    async function loadDistilledFiles() {
                      const root = document.getElementById("distilledFiles");
                      const editor = document.getElementById("distilledEditor");
                      const label = document.getElementById("distilledFileName");
                      currentDistilledFile = "";
                      editor.value = "";
                      label.textContent = "No file selected";
                      if (!currentAgent) {
                        root.textContent = "Select an agent to load distilled files.";
                        return;
                      }
                      const data = await api("/api/distilled?id=" + encodeURIComponent(currentAgent));
                      root.innerHTML = "";
                      if (!data.files.length) {
                        root.textContent = "No distilled files found for this agent.";
                        return;
                      }
                      data.files.forEach(file => {
                        const button = document.createElement("button");
                        button.className = "tab-button";
                        button.textContent = file.name;
                        button.onclick = () => selectDistilledFile(file.name);
                        root.appendChild(button);
                      });
                      await selectDistilledFile(data.files[0].name);
                    }

                    async function saveAgentFile(type, editorId, successMessage) {
                      if (!currentAgent) {
                        setFlash("Select an agent first.");
                        return;
                      }
                      const content = document.getElementById(editorId).value;
                      await api("/api/agent-file?id=" + encodeURIComponent(currentAgent) + "&type=" + encodeURIComponent(type), {
                        method: "PUT",
                        body: JSON.stringify({ content })
                      });
                      if (currentAgentDetails) {
                        currentAgentDetails[type] = content;
                        if (type === "status") {
                          document.getElementById("agentStatus").textContent = content || "No status.";
                        }
                        renderAgentDetails();
                      }
                      setFlash(successMessage);
                    }

                    async function saveAgentSettings() {
                      if (!currentAgent) {
                        setFlash("Select an agent first.");
                        return;
                      }
                      const executionState = document.getElementById("executionStateSelect").value;
                      const runIntervalSeconds = document.getElementById("runIntervalInput").value.trim() || "300";
                      await api("/api/agent-settings?id=" + encodeURIComponent(currentAgent), {
                        method: "PUT",
                        body: JSON.stringify({ executionState, runIntervalSeconds })
                      });
                      setFlash("Saved agent settings.");
                      const details = await api("/api/agent?id=" + encodeURIComponent(currentAgent));
                      currentAgentDetails = details;
                      document.getElementById("agentStatus").textContent = details.status || "No status.";
                      renderAgentDetails();
                      await loadAgents();
                    }

                    async function selectDistilledFile(name) {
                      currentDistilledFile = name;
                      document.querySelectorAll("#distilledFiles button").forEach(button => {
                        button.classList.toggle("active", button.textContent === name);
                      });
                      const data = await api("/api/distilled/file?id=" + encodeURIComponent(currentAgent) + "&name=" + encodeURIComponent(name));
                      document.getElementById("distilledFileName").textContent = data.name;
                      document.getElementById("distilledEditor").value = data.content || "";
                    }

                    async function saveDistilledFile() {
                      if (!currentAgent || !currentDistilledFile) {
                        setFlash("Select a distilled file first.");
                        return;
                      }
                      const content = document.getElementById("distilledEditor").value;
                      await api("/api/distilled/file?id=" + encodeURIComponent(currentAgent) + "&name=" + encodeURIComponent(currentDistilledFile), {
                        method: "PUT",
                        body: JSON.stringify({ content })
                      });
                      setFlash("Saved distilled file.");
                    }

                    async function loadCompanySources() {
                      const data = await api("/api/company-sources");
                      const root = document.getElementById("companySources");
                      root.innerHTML = "";
                      if (!data.sources.length) {
                        root.textContent = "No company data directories configured.";
                        return;
                      }
                      data.sources.forEach(source => {
                        const row = document.createElement("div");
                        row.className = "source-item";

                        const label = document.createElement("div");
                        label.textContent = source.index + ". " + source.path;

                        const edit = document.createElement("button");
                        edit.className = "mini";
                        edit.textContent = "Edit";
                        edit.onclick = async () => {
                          const next = window.prompt("Replace path", source.path);
                          if (!next) return;
                          await api("/api/company-sources/" + source.index, {
                            method: "PUT",
                            body: JSON.stringify({ path: next })
                          });
                          setFlash("Updated company data source.");
                          loadCompanySources();
                        };

                        const remove = document.createElement("button");
                        remove.className = "mini";
                        remove.textContent = "Delete";
                        remove.onclick = async () => {
                          await api("/api/company-sources/" + source.index, { method: "DELETE" });
                          setFlash("Removed company data source.");
                          loadCompanySources();
                        };

                        row.appendChild(label);
                        row.appendChild(edit);
                        row.appendChild(remove);
                        root.appendChild(row);
                      });
                    }

                    async function loadUsage() {
                      const data = await api("/api/usage");
                      const root = document.getElementById("usageBox");
                      if (!data.usage.length) {
                        root.textContent = "No usage recorded yet.";
                        return;
                      }
                      root.innerHTML = "";
                      data.usage.forEach(item => {
                        const row = document.createElement("div");
                        row.className = "detail-box";
                        row.innerHTML =
                          "<strong>" + item.provider + " / " + item.model + "</strong>" +
                          "<div>daily: " + item.dailyTotalTokens + " tokens</div>" +
                          "<div>monthly: " + item.monthlyTotalTokens + " tokens</div>" +
                          "<div>yearly: " + item.yearlyTotalTokens + " tokens</div>" +
                          "<div>requests: " + item.requests + " | exact: " + (item.allExact ? "yes" : "mixed/estimated") + "</div>";
                        root.appendChild(row);
                      });
                    }

                    async function sendPrompt() {
                      const prompt = document.getElementById("prompt").value.trim();
                      const mode = document.getElementById("mode").value;
                      if (!prompt) {
                        setFlash("Prompt is required.");
                        return;
                      }
                      setFlash("Running...");
                      document.getElementById("responseBox").textContent = "Working...";
                      try {
                        const data = await api("/api/respond", {
                          method: "POST",
                          body: JSON.stringify({ agentId: currentAgent, mode, prompt })
                        });
                        document.getElementById("responseBox").textContent = data.response;
                        setFlash("");
                        loadUsage();
                      } catch (error) {
                        document.getElementById("responseBox").textContent = error.message;
                        setFlash("Request failed.");
                      }
                    }

                    document.querySelectorAll(".tab-button").forEach(button => {
                      button.onclick = () => activateTab(button.dataset.tab);
                    });
                    document.getElementById("send").onclick = sendPrompt;
                    document.getElementById("saveSettings").onclick = saveAgentSettings;
                    document.getElementById("saveRole").onclick = () => saveAgentFile("role", "roleEditor", "Saved role.");
                    document.getElementById("saveStatus").onclick = () => saveAgentFile("status", "statusEditor", "Saved status.");
                    document.getElementById("saveDistill").onclick = () => saveAgentFile("distill", "distillEditor", "Saved distill instructions.");
                    document.getElementById("saveDistilled").onclick = saveDistilledFile;
                    document.getElementById("addSource").onclick = async () => {
                      const input = document.getElementById("companyPath");
                      const path = input.value.trim();
                      if (!path) {
                        setFlash("Path is required.");
                        return;
                      }
                      await api("/api/company-sources", {
                        method: "POST",
                        body: JSON.stringify({ path })
                      });
                      input.value = "";
                      setFlash("Added company data source.");
                      loadCompanySources();
                    };

                    loadAgents().catch(error => setFlash(error.message));
                    loadCompanySources().catch(error => setFlash(error.message));
                    loadUsage().catch(error => setFlash(error.message));
                  </script>
                </body>
                </html>
                """;
    }
}
