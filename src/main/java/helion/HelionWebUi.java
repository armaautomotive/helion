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
                    .badge.run-running {
                      background: #edf8f1;
                      border-color: #8dc5a0;
                      color: #1e6b39;
                    }
                    .badge.run-paused, .badge.runtime-idle {
                      background: #f6f0e5;
                      border-color: #cfb98e;
                      color: #7d5b18;
                    }
                    .badge.run-disabled {
                      background: #f1eceb;
                      border-color: #c5b0ac;
                      color: #765650;
                    }
                    .badge.target-cloud {
                      background: #e9f3ff;
                      border-color: #8ab3e6;
                      color: #1d4f85;
                    }
                    .badge.target-local {
                      background: #edf8f1;
                      border-color: #8dc5a0;
                      color: #1e6b39;
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
                    .session-meta {
                      display: grid;
                      gap: .45rem;
                    }
                    .session-actions {
                      display: flex;
                      flex-wrap: wrap;
                      gap: .5rem;
                      align-items: center;
                    }
                    .toggle-button {
                      border: 1px solid var(--line);
                      border-radius: .8rem;
                      background: white;
                      padding: .55rem .8rem;
                      cursor: pointer;
                      font: inherit;
                    }
                    .toggle-button.run {
                      background: #edf8f1;
                      border-color: #8dc5a0;
                      color: #1e6b39;
                    }
                    .toggle-button.pause {
                      background: #f6f0e5;
                      border-color: #cfb98e;
                      color: #7d5b18;
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
                    .usage-grid {
                      display: grid;
                      gap: 1rem;
                    }
                    .activity-grid {
                      display: grid;
                      grid-template-columns: minmax(18rem, 22rem) 1fr;
                      gap: 1rem;
                      min-height: 26rem;
                      align-items: start;
                    }
                    .activity-list {
                      background: rgba(255,255,255,.5);
                      border: 1px solid var(--line);
                      border-radius: .9rem;
                      padding: .75rem;
                      overflow: auto;
                      align-self: stretch;
                    }
                    .activity-list button {
                      width: 100%;
                      text-align: left;
                      margin-bottom: .45rem;
                    }
                    .activity-list button:last-child {
                      margin-bottom: 0;
                    }
                    .activity-card {
                      display: grid;
                      gap: .3rem;
                    }
                    .activity-header {
                      display: flex;
                      justify-content: space-between;
                      align-items: center;
                      gap: .5rem;
                    }
                    .activity-summary {
                      font-size: .86rem;
                      color: var(--muted);
                    }
                    .activity-detail-grid {
                      display: grid;
                      gap: .75rem;
                      align-content: start;
                    }
                    .activity-meta-grid {
                      display: grid;
                      grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr));
                      gap: .6rem;
                    }
                    .activity-meta-card {
                      border: 1px solid var(--line);
                      border-radius: .8rem;
                      padding: .7rem;
                      background: rgba(255,255,255,.78);
                    }
                    .activity-meta-card strong {
                      display: block;
                      font-size: .76rem;
                      text-transform: uppercase;
                      letter-spacing: .06em;
                      color: var(--muted);
                      margin-bottom: .25rem;
                    }
                    .activity-section {
                      border: 1px solid var(--line);
                      border-radius: .8rem;
                      padding: .8rem;
                      background: rgba(255,255,255,.78);
                      align-self: start;
                    }
                    .activity-section h3 {
                      font-size: .82rem;
                      text-transform: uppercase;
                      letter-spacing: .06em;
                      color: var(--muted);
                      margin-bottom: .45rem;
                    }
                    .activity-listing {
                      display: grid;
                      gap: .35rem;
                      font-size: .9rem;
                    }
                    .activity-listing div {
                      padding: .45rem .55rem;
                      border-radius: .55rem;
                      background: rgba(244,239,230,.65);
                    }
                    .activity-text-block {
                      white-space: pre-wrap;
                      font-size: .92rem;
                      line-height: 1.42;
                      max-height: 18rem;
                      overflow: auto;
                    }
                    .chart-box {
                      border: 1px solid var(--line);
                      border-radius: .9rem;
                      padding: .9rem;
                      background: rgba(255,255,255,.72);
                      display: grid;
                      gap: .75rem;
                      align-self: start;
                    }
                    .chart-legend {
                      display: flex;
                      flex-wrap: wrap;
                      gap: .75rem;
                      font-size: .84rem;
                      color: var(--muted);
                    }
                    .legend-item {
                      display: inline-flex;
                      align-items: center;
                      gap: .35rem;
                    }
                    .legend-swatch {
                      width: .9rem;
                      height: .2rem;
                      border-radius: 999px;
                      display: inline-block;
                    }
                    .legend-swatch.activity {
                      background: var(--accent);
                    }
                    .legend-swatch.success {
                      background: #1e6b39;
                    }
                    .legend-swatch.failure {
                      background: #a1362c;
                    }
                    .legend-swatch.tokens {
                      background: var(--accent-2);
                    }
                    .chart-caption {
                      font-size: .8rem;
                      color: var(--muted);
                    }
                    .badge.level-success {
                      background: #edf8f1;
                      border-color: #8dc5a0;
                      color: #1e6b39;
                    }
                    .badge.level-error {
                      background: #fff0ef;
                      border-color: #dd8d87;
                      color: #a1362c;
                    }
                    .badge.level-info {
                      background: #edf8f8;
                      border-color: #7cbeb6;
                      color: #0f6f78;
                    }
                    @media (max-width: 1100px) {
                      .app { grid-template-columns: 1fr; }
                      .content-grid { grid-template-columns: 1fr; }
                      .role-grid { grid-template-columns: 1fr; }
                      .activity-grid { grid-template-columns: 1fr; }
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
                        <div class="session-meta">
                          <div>
                          <h2>Session</h2>
                          <div class="muted" id="sessionLabel">No agent selected</div>
                          </div>
                          <div class="session-actions">
                            <span class="badge" id="runStateBadge">run state unknown</span>
                            <button class="toggle-button run" id="runAgentButton">Run</button>
                            <button class="toggle-button pause" id="pauseAgentButton">Pause</button>
                          </div>
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
                          <button class="tab-button" data-tab="activity">Activity</button>
                          <button class="tab-button" data-tab="output">Output</button>
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
                                <strong>Run State</strong>
                                <button class="mini" id="saveSettings">Save</button>
                              </div>
                              <select id="runStateSelect">
                                <option value="running">running</option>
                                <option value="paused">paused</option>
                                <option value="disabled">disabled</option>
                              </select>
                            </div>
                            <div class="editor-box compact">
                              <div class="editor-title">
                                <strong>Execution Target</strong>
                              </div>
                              <select id="executionTargetSelect">
                                <option value="cloud">cloud</option>
                                <option value="local">local</option>
                              </select>
                            </div>
                            <div class="editor-box compact">
                              <div class="editor-title">
                                <strong>Run Interval Seconds</strong>
                              </div>
                              <input id="runIntervalInput" type="number" min="10" step="10" placeholder="300">
                            </div>
                            <div class="editor-box compact">
                              <div class="editor-title">
                                <strong>Preferred Local Pool</strong>
                              </div>
                              <select id="preferredLocalPoolSelect"></select>
                            </div>
                            <div class="editor-box compact">
                              <div class="editor-title">
                                <strong>Primary Output File</strong>
                              </div>
                              <input id="primaryOutputFileInput" type="text" placeholder="workspace/prospects.md">
                            </div>
                          </div>
                          <div class="detail-box" id="settingsHelp">Agent settings control whether autonomous work runs and how often the supervisor checks this agent.</div>
                        </div>

                        <div class="tab-panel" data-panel="activity">
                          <div class="activity-grid">
                            <div class="activity-list" id="activityList">Select an agent to load activity.</div>
                            <div class="stack">
                              <div class="detail-box" id="activitySummary">Autonomous runs, searches, results, and failures will appear here.</div>
                              <div class="detail-box" id="activityDetails">No activity entry selected.</div>
                              <div class="chart-box">
                                <strong>Last 24 Hours</strong>
                                <svg id="activityChart24h" viewBox="0 0 640 220" width="100%" height="220" aria-label="Agent activity 24 hour chart"></svg>
                                <div class="chart-legend">
                                  <span class="legend-item"><span class="legend-swatch success"></span> success</span>
                                  <span class="legend-item"><span class="legend-swatch failure"></span> failure</span>
                                  <span class="legend-item"><span class="legend-swatch tokens"></span> estimated token load</span>
                                </div>
                                <div class="chart-caption" id="activityChartCaption24h">Activity and estimated token usage over the last 24 hours.</div>
                              </div>
                              <div class="chart-box">
                                <strong>Recent Activity</strong>
                                <svg id="activityChart" viewBox="0 0 640 220" width="100%" height="220" aria-label="Agent activity chart"></svg>
                                <div class="chart-legend">
                                  <span class="legend-item"><span class="legend-swatch success"></span> success</span>
                                  <span class="legend-item"><span class="legend-swatch failure"></span> failure</span>
                                  <span class="legend-item"><span class="legend-swatch tokens"></span> estimated token load</span>
                                </div>
                                <div class="chart-caption" id="activityChartCaption">Activity and estimated token usage over the last 14 days.</div>
                              </div>
                            </div>
                          </div>
                        </div>

                        <div class="tab-panel" data-panel="output">
                          <div class="content-grid">
                            <div class="file-list" id="outputFiles">Select an agent to load output files.</div>
                            <div class="stack">
                              <div class="detail-box" id="outputSummary">Select an agent to load its output files.</div>
                              <div class="editor-box">
                                <div class="editor-title">
                                  <strong id="outputFileLabel">No output file selected</strong>
                                  <button class="mini" id="saveOutput">Save</button>
                                </div>
                                <textarea id="outputEditor" placeholder="The selected agent's deliverable file will appear here."></textarea>
                              </div>
                            </div>
                          </div>
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
                          <div class="usage-grid">
                            <div class="chart-box">
                              <strong>Hourly Usage</strong>
                              <svg id="usageHourlyChart" viewBox="0 0 640 220" width="100%" height="220" aria-label="Hourly usage chart"></svg>
                              <div class="chart-caption" id="usageHourlyCaption">Token usage and request count over the last 24 hours.</div>
                            </div>
                            <div class="chart-box">
                              <strong>Daily Usage</strong>
                              <svg id="usageDailyChart" viewBox="0 0 640 220" width="100%" height="220" aria-label="Daily usage chart"></svg>
                              <div class="chart-caption" id="usageDailyCaption">Token usage and request count over the last 30 days.</div>
                            </div>
                            <div class="usage-box usage-list" id="usageBox">Loading...</div>
                          </div>
                        </div>
                      </section>
                    </main>
                  </div>
                  <script>
                    let currentAgent = "";
                    let currentDistilledFile = "";
                    let currentOutputFile = "";
                    let currentAgentDetails = null;
                    let currentActivityEntries = [];
                    let currentActivitySelectionKey = "";
                    const chatStateByAgent = {};
                    let activityRefreshHandle = null;

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
                      const runStateSelect = document.getElementById("runStateSelect");
                      const executionTargetSelect = document.getElementById("executionTargetSelect");
                      const runIntervalInput = document.getElementById("runIntervalInput");
                      const preferredLocalPoolSelect = document.getElementById("preferredLocalPoolSelect");
                      const primaryOutputFileInput = document.getElementById("primaryOutputFileInput");
                      const settingsHelp = document.getElementById("settingsHelp");
                      const roleHelp = document.getElementById("roleHelp");
                      const distilledRoot = document.getElementById("distilledDetails");
                      const runStateBadge = document.getElementById("runStateBadge");
                      if (!currentAgentDetails) {
                        roleEditor.value = "";
                        statusEditor.value = "";
                        distillEditor.value = "";
                        runStateSelect.value = "running";
                        executionTargetSelect.value = "local";
                        runIntervalInput.value = "";
                        preferredLocalPoolSelect.innerHTML = "<option value='default'>default</option>";
                        preferredLocalPoolSelect.value = "default";
                        primaryOutputFileInput.value = "";
                        runStateBadge.textContent = "run state unknown";
                        settingsHelp.textContent = "Agent settings control whether autonomous work runs and how often the supervisor checks this agent.";
                        roleHelp.textContent = "Role defines the agent job, boundaries, and expected outputs.";
                        distilledRoot.textContent = "Select an agent to view workspace context.";
                        return;
                      }
                      roleEditor.value = currentAgentDetails.role || "";
                      statusEditor.value = currentAgentDetails.status || "";
                      distillEditor.value = currentAgentDetails.distill || "";
                      runStateSelect.value = currentAgentDetails.runState || "running";
                      executionTargetSelect.value = currentAgentDetails.executionTarget || "local";
                      runIntervalInput.value = readRunInterval(currentAgentDetails.status || "") || "";
                      const localPools = (currentAgentDetails.localPools && currentAgentDetails.localPools.length)
                        ? currentAgentDetails.localPools
                        : [currentAgentDetails.defaultLocalPool || "default"];
                      preferredLocalPoolSelect.innerHTML = "";
                      localPools.forEach(poolName => {
                        const option = document.createElement("option");
                        option.value = poolName;
                        option.textContent = poolName;
                        preferredLocalPoolSelect.appendChild(option);
                      });
                      preferredLocalPoolSelect.value = currentAgentDetails.preferredLocalPool || currentAgentDetails.defaultLocalPool || "default";
                      primaryOutputFileInput.value = currentAgentDetails.primaryOutputFile || "";
                      runStateBadge.textContent = "run state " + (currentAgentDetails.runState || "running");
                      runStateBadge.className = "badge run-" + (currentAgentDetails.runState || "running");
                      settingsHelp.textContent =
                        "Current run state: " + (currentAgentDetails.runState || "running") +
                        "\\nExecution target: " + (currentAgentDetails.executionTarget || "local") +
                        "\\nPreferred local pool: " + (currentAgentDetails.preferredLocalPool || currentAgentDetails.defaultLocalPool || "default") +
                        "\\nCurrent interval: " + (runIntervalInput.value || "300") + " seconds" +
                        "\\nPrimary output: " + (currentAgentDetails.primaryOutputFile || "(none)");
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
                      if (tabName === "activity") {
                        loadActivity().catch(error => setFlash(error.message));
                      }
                      syncActivityAutoRefresh();
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
                          "<div class='agent-topline'><strong>" + agent.id + "</strong><span class='badge run-" + agent.runState + "'>" + agent.runState + "</span></div>" +
                          "<div class='agent-meta'>" +
                          "<span class='badge target-" + agent.executionTarget + "'>" + agent.executionTarget + "</span>" +
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
                      persistChatState();
                      currentAgent = agentId;
                      await loadAgentButtons();
                      const details = await api("/api/agent?id=" + encodeURIComponent(agentId));
                      currentAgentDetails = details;
                      document.getElementById("sessionLabel").textContent = "Agent: " + agentId;
                      document.getElementById("agentStatus").textContent = details.status || "No status.";
                      restoreChatState();
                      renderAgentDetails();
                      await loadOutput();
                      await loadDistilledFiles();
                      await loadActivity();
                      await loadUsage();
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

                    function chatStateKey() {
                      return currentAgent || "__default__";
                    }

                    function persistChatState() {
                      const key = chatStateKey();
                      chatStateByAgent[key] = {
                        prompt: document.getElementById("prompt").value || "",
                        response: document.getElementById("responseBox").textContent || "Response output will appear here."
                      };
                    }

                    function restoreChatState() {
                      const state = chatStateByAgent[chatStateKey()];
                      document.getElementById("prompt").value = state && state.prompt ? state.prompt : "";
                      document.getElementById("responseBox").textContent = state && state.response
                        ? state.response
                        : "Response output will appear here.";
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

                    function formatActivityTimestamp(value) {
                      if (!value) {
                        return "unknown time";
                      }
                      return value.replace("T", " ");
                    }

                    function renderActivityEntry(entry) {
                      const summary = entry && entry.summary ? entry.summary : "No summary.";
                      const parsed = parseActivityDetails(entry && entry.details ? entry.details : "");
                      currentActivitySelectionKey = activityEntryKey(entry);
                      document.getElementById("activitySummary").innerHTML =
                        "<div class='activity-detail-grid'>" +
                          "<div class='activity-meta-grid'>" +
                            metaCard("Task", entry && entry.task ? entry.task : "(none)") +
                            metaCard("Level", entry && entry.level ? entry.level : "info") +
                            metaCard("Time", formatActivityTimestamp(entry && entry.timestamp ? entry.timestamp : "")) +
                            metaCard("Summary", summary) +
                          "</div>" +
                        "</div>";
                      document.getElementById("activityDetails").innerHTML = renderActivityDetails(parsed);
                    }

                    function metaCard(label, value) {
                      return "<div class='activity-meta-card'><strong>" + escapeHtml(label) + "</strong><span>" + escapeHtml(value || "(none)") + "</span></div>";
                    }

                    function parseActivityDetails(text) {
                      const lines = (text || "").split("\\n");
                      const fields = [];
                      const listItems = [];
                      const freeText = [];
                      lines.forEach(line => {
                        const trimmed = line.trim();
                        if (!trimmed) {
                          return;
                        }
                        if (trimmed.startsWith("- ")) {
                          listItems.push(trimmed.substring(2).trim());
                          return;
                        }
                        const colon = trimmed.indexOf(":");
                        if (colon > 0 && colon < 40) {
                          const key = trimmed.substring(0, colon).trim();
                          const value = trimmed.substring(colon + 1).trim();
                          if (value) {
                            fields.push({ key, value });
                            return;
                          }
                        }
                        freeText.push(trimmed);
                      });
                      return { fields, listItems, freeText, raw: text || "" };
                    }

                    function renderActivityDetails(parsed) {
                      let html = "<div class='activity-detail-grid'>";
                      if (parsed.fields.length) {
                        html += "<div class='activity-section'><h3>Fields</h3><div class='activity-meta-grid'>" +
                          parsed.fields.map(field => metaCard(field.key, field.value)).join("") +
                          "</div></div>";
                      }
                      if (parsed.listItems.length) {
                        html += "<div class='activity-section'><h3>Evidence</h3><div class='activity-listing'>" +
                          parsed.listItems.map(item => "<div>" + escapeHtml(item) + "</div>").join("") +
                          "</div></div>";
                      }
                      if (parsed.freeText.length) {
                        html += "<div class='activity-section'><h3>Notes</h3><div class='activity-text-block'>" +
                          escapeHtml(parsed.freeText.join("\\n")) +
                          "</div></div>";
                      }
                      if (!parsed.fields.length && !parsed.listItems.length && !parsed.freeText.length) {
                        html += "<div class='activity-section'><h3>Details</h3><div class='activity-text-block'>No activity details.</div></div>";
                      }
                      html += "</div>";
                      return html;
                    }

                    function estimateTokensForEntry(entry) {
                      const level = (entry.level || "").toLowerCase();
                      const executionTarget = ((currentAgentDetails && currentAgentDetails.executionTarget) || "local").toLowerCase();
                      const successBase = executionTarget === "cloud" ? 1600 : 900;
                      const errorBase = executionTarget === "cloud" ? 450 : 220;
                      return level === "error" ? errorBase : successBase;
                    }

                    function renderActivityChart(entries) {
                      renderActivityDailyChart(entries);
                      renderActivityHourlyChart(entries);
                    }

                    function renderActivityDailyChart(entries) {
                      const days = 14;
                      const today = new Date();
                      today.setHours(0, 0, 0, 0);
                      const buckets = [];
                      for (let i = days - 1; i >= 0; i--) {
                        const day = new Date(today);
                        day.setDate(today.getDate() - i);
                        buckets.push({
                          key: day.toISOString().slice(0, 10),
                          label: day.toLocaleDateString(undefined, { month: "short", day: "numeric" }),
                          success: 0,
                          failure: 0,
                          tokens: 0
                        });
                      }
                      const byKey = new Map(buckets.map(bucket => [bucket.key, bucket]));
                      (entries || []).forEach(entry => {
                        if (!entry.timestamp) {
                          return;
                        }
                        const dayKey = entry.timestamp.slice(0, 10);
                        const bucket = byKey.get(dayKey);
                        if (!bucket) {
                          return;
                        }
                        if ((entry.level || "").toLowerCase() === "error") {
                          bucket.failure += 1;
                        } else if ((entry.level || "").toLowerCase() === "success") {
                          bucket.success += 1;
                        }
                        bucket.tokens += estimateTokensForEntry(entry);
                      });
                      renderDualAxisActivityChart("activityChart", "activityChartCaption", buckets, "Last 14 days", bucket => bucket.label, true);
                    }

                    function renderActivityHourlyChart(entries) {
                      const hours = 24;
                      const now = new Date();
                      now.setMinutes(0, 0, 0);
                      const buckets = [];
                      for (let i = hours - 1; i >= 0; i--) {
                        const hour = new Date(now);
                        hour.setHours(now.getHours() - i);
                        buckets.push({
                          key: hour.toISOString().slice(0, 13),
                          label: hour.toLocaleTimeString([], { hour: "numeric" }),
                          success: 0,
                          failure: 0,
                          tokens: 0
                        });
                      }
                      const byKey = new Map(buckets.map(bucket => [bucket.key, bucket]));
                      (entries || []).forEach(entry => {
                        if (!entry.timestamp) {
                          return;
                        }
                        const hourKey = entry.timestamp.slice(0, 13);
                        const bucket = byKey.get(hourKey);
                        if (!bucket) {
                          return;
                        }
                        if ((entry.level || "").toLowerCase() === "error") {
                          bucket.failure += 1;
                        } else if ((entry.level || "").toLowerCase() === "success") {
                          bucket.success += 1;
                        }
                        bucket.tokens += estimateTokensForEntry(entry);
                      });
                      renderDualAxisActivityChart("activityChart24h", "activityChartCaption24h", buckets, "Last 24 hours", bucket => bucket.label, false);
                    }

                    function renderDualAxisActivityChart(svgId, captionId, buckets, horizonLabel, labelFn, sparseLabels) {
                      const svg = document.getElementById(svgId);
                      const caption = document.getElementById(captionId);
                      const width = 640;
                      const height = 220;
                      const padLeft = 42;
                      const padRight = 42;
                      const padTop = 16;
                      const padBottom = 34;
                      const innerWidth = width - padLeft - padRight;
                      const innerHeight = height - padTop - padBottom;

                      const maxActivity = Math.max(1, ...buckets.map(bucket => Math.max(bucket.success, bucket.failure)));
                      const maxTokens = Math.max(1, ...buckets.map(bucket => bucket.tokens));
                      const stepX = buckets.length > 1 ? innerWidth / (buckets.length - 1) : innerWidth;
                      const successPoints = [];
                      const failurePoints = [];
                      const tokenPoints = [];
                      let labels = "";
                      let grid = "";
                      let countAxisLabels = "";
                      let tokenAxisLabels = "";

                      buckets.forEach((bucket, index) => {
                        const x = padLeft + stepX * index;
                        const successY = padTop + innerHeight - (bucket.success / maxActivity) * innerHeight;
                        const failureY = padTop + innerHeight - (bucket.failure / maxActivity) * innerHeight;
                        const tokenY = padTop + innerHeight - (bucket.tokens / maxTokens) * innerHeight;
                        successPoints.push(x.toFixed(1) + "," + successY.toFixed(1));
                        failurePoints.push(x.toFixed(1) + "," + failureY.toFixed(1));
                        tokenPoints.push(x.toFixed(1) + "," + tokenY.toFixed(1));
                        const shouldLabel = sparseLabels
                          ? (index % 2 === 0 || index === buckets.length - 1)
                          : (index % 3 === 0 || index === buckets.length - 1);
                        if (shouldLabel) {
                          labels += "<text x='" + x.toFixed(1) + "' y='" + (height - 10) + "' text-anchor='middle' font-size='11' fill='#5d6a6f'>" + escapeHtml(labelFn(bucket)) + "</text>";
                        }
                      });

                      for (let i = 0; i <= 4; i++) {
                        const y = padTop + (innerHeight / 4) * i;
                        grid += "<line x1='" + padLeft + "' y1='" + y.toFixed(1) + "' x2='" + (width - padRight) + "' y2='" + y.toFixed(1) + "' stroke='rgba(93,106,111,0.14)' stroke-width='1' />";
                        const countValue = Math.round(maxActivity * (1 - i / 4));
                        const tokenValue = Math.round(maxTokens * (1 - i / 4));
                        countAxisLabels += "<text x='" + (padLeft - 8) + "' y='" + (y + 4).toFixed(1) + "' text-anchor='end' font-size='11' fill='#1e6b39'>" + countValue + "</text>";
                        tokenAxisLabels += "<text x='" + (width - padRight + 8) + "' y='" + (y + 4).toFixed(1) + "' text-anchor='start' font-size='11' fill='#c96d31'>" + tokenValue + "</text>";
                      }

                      svg.innerHTML =
                        "<rect x='0' y='0' width='" + width + "' height='" + height + "' rx='12' fill='rgba(255,255,255,0.2)'></rect>" +
                        grid +
                        "<text x='12' y='18' font-size='11' fill='#1e6b39'>success/failure</text>" +
                        "<text x='" + (width - 52) + "' y='18' font-size='11' fill='#c96d31'>tokens</text>" +
                        countAxisLabels +
                        tokenAxisLabels +
                        "<polyline fill='none' stroke='#1e6b39' stroke-width='3' stroke-linecap='round' stroke-linejoin='round' points='" + successPoints.join(" ") + "'></polyline>" +
                        "<polyline fill='none' stroke='#a1362c' stroke-width='3' stroke-linecap='round' stroke-linejoin='round' points='" + failurePoints.join(" ") + "'></polyline>" +
                        "<polyline fill='none' stroke='#c96d31' stroke-width='3' stroke-linecap='round' stroke-linejoin='round' points='" + tokenPoints.join(" ") + "'></polyline>" +
                        labels;

                      const totalSuccess = buckets.reduce((sum, bucket) => sum + bucket.success, 0);
                      const totalFailure = buckets.reduce((sum, bucket) => sum + bucket.failure, 0);
                      const totalTokens = buckets.reduce((sum, bucket) => sum + bucket.tokens, 0);
                      caption.textContent =
                        horizonLabel + ": " + totalSuccess + " success, " + totalFailure + " failure, estimated " + totalTokens + " tokens. Token line is approximate.";
                    }

                    function renderUsageChart(svgId, captionId, points, horizonLabel) {
                      const svg = document.getElementById(svgId);
                      const caption = document.getElementById(captionId);
                      const width = 640;
                      const height = 220;
                      const padLeft = 42;
                      const padRight = 20;
                      const padTop = 16;
                      const padBottom = 34;
                      const innerWidth = width - padLeft - padRight;
                      const innerHeight = height - padTop - padBottom;
                      const safePoints = points && points.length ? points : [{ label: "", totalTokens: 0, requests: 0 }];
                      const maxTokens = Math.max(1, ...safePoints.map(point => point.totalTokens || 0));
                      const maxRequests = Math.max(1, ...safePoints.map(point => point.requests || 0));
                      const stepX = safePoints.length > 1 ? innerWidth / (safePoints.length - 1) : innerWidth;
                      const tokenPoints = [];
                      const requestPoints = [];
                      let labels = "";
                      let grid = "";

                      safePoints.forEach((point, index) => {
                        const x = padLeft + stepX * index;
                        const tokenY = padTop + innerHeight - ((point.totalTokens || 0) / maxTokens) * innerHeight;
                        const requestY = padTop + innerHeight - ((point.requests || 0) / maxRequests) * innerHeight;
                        tokenPoints.push(x.toFixed(1) + "," + tokenY.toFixed(1));
                        requestPoints.push(x.toFixed(1) + "," + requestY.toFixed(1));
                        const interval = safePoints.length > 24 ? 4 : 3;
                        if (index % interval === 0 || index === safePoints.length - 1) {
                          labels += "<text x='" + x.toFixed(1) + "' y='" + (height - 10) + "' text-anchor='middle' font-size='11' fill='#5d6a6f'>" + escapeHtml(point.label || "") + "</text>";
                        }
                      });

                      for (let i = 0; i <= 4; i++) {
                        const y = padTop + (innerHeight / 4) * i;
                        grid += "<line x1='" + padLeft + "' y1='" + y.toFixed(1) + "' x2='" + (width - padRight) + "' y2='" + y.toFixed(1) + "' stroke='rgba(93,106,111,0.14)' stroke-width='1' />";
                      }

                      svg.innerHTML =
                        "<rect x='0' y='0' width='" + width + "' height='" + height + "' rx='12' fill='rgba(255,255,255,0.2)'></rect>" +
                        grid +
                        "<polyline fill='none' stroke='#0f6f78' stroke-width='3' stroke-linecap='round' stroke-linejoin='round' points='" + tokenPoints.join(" ") + "'></polyline>" +
                        "<polyline fill='none' stroke='#c96d31' stroke-width='3' stroke-linecap='round' stroke-linejoin='round' points='" + requestPoints.join(" ") + "'></polyline>" +
                        labels;

                      const totalTokens = safePoints.reduce((sum, point) => sum + (point.totalTokens || 0), 0);
                      const totalRequests = safePoints.reduce((sum, point) => sum + (point.requests || 0), 0);
                      caption.textContent = horizonLabel + ": " + totalTokens + " tokens across " + totalRequests + " requests.";
                    }

                    function activityEntryKey(entry) {
                      if (!entry) {
                        return "";
                      }
                      return [
                        entry.timestamp || "",
                        entry.level || "",
                        entry.task || "",
                        entry.summary || ""
                      ].join("|");
                    }

                    async function loadActivity() {
                      const list = document.getElementById("activityList");
                      const summary = document.getElementById("activitySummary");
                      const details = document.getElementById("activityDetails");
                      currentActivityEntries = [];
                      if (!currentAgent) {
                        list.textContent = "Select an agent to load activity.";
                        summary.textContent = "Autonomous runs, searches, results, and failures will appear here.";
                        details.textContent = "No activity entry selected.";
                        renderActivityChart([]);
                        return;
                      }
                      const data = await api("/api/activity?id=" + encodeURIComponent(currentAgent) + "&_ts=" + Date.now());
                      currentActivityEntries = (data.entries || []).slice().sort((a, b) => {
                        const left = a && a.timestamp ? a.timestamp : "";
                        const right = b && b.timestamp ? b.timestamp : "";
                        return right.localeCompare(left);
                      });
                      renderActivityChart(currentActivityEntries);
                      list.innerHTML = "";
                      if (!currentActivityEntries.length) {
                        list.textContent = "No activity recorded yet for this agent.";
                        summary.textContent = "No recent activity.";
                        details.textContent = "When the agent runs, entries will appear here.";
                        return;
                      }
                      const selectedKey = currentActivitySelectionKey;
                      let selectedEntry = currentActivityEntries.find(entry => activityEntryKey(entry) === selectedKey);
                      if (!selectedEntry) {
                        selectedEntry = currentActivityEntries[0];
                      }
                      currentActivityEntries.forEach((entry, index) => {
                        const button = document.createElement("button");
                        const isSelected = activityEntryKey(entry) === activityEntryKey(selectedEntry);
                        button.className = isSelected ? "tab-button active" : "tab-button";
                        button.onclick = () => {
                          document.querySelectorAll("#activityList button").forEach(node => node.classList.remove("active"));
                          button.classList.add("active");
                          renderActivityEntry(entry);
                        };
                        button.innerHTML =
                          "<div class='activity-card'>" +
                          "<div class='activity-header'><strong>" + escapeHtml(formatActivityTimestamp(entry.timestamp)) + "</strong><span class='badge level-" + escapeHtml(entry.level || "info") + "'>" + escapeHtml(entry.level || "info") + "</span></div>" +
                          "<div>" + escapeHtml(entry.task || "(none)") + "</div>" +
                          "<div class='activity-summary'>" + escapeHtml(entry.summary || "No summary.") + "</div>" +
                          "</div>";
                        list.appendChild(button);
                      });
                      renderActivityEntry(selectedEntry);
                    }

                    function isActivityTabActive() {
                      const panel = document.querySelector(".tab-panel[data-panel='activity']");
                      return !!panel && panel.classList.contains("active");
                    }

                    function syncActivityAutoRefresh() {
                      if (activityRefreshHandle) {
                        window.clearInterval(activityRefreshHandle);
                        activityRefreshHandle = null;
                      }
                      if (!currentAgent || !isActivityTabActive()) {
                        return;
                      }
                      activityRefreshHandle = window.setInterval(() => {
                        loadActivity().catch(error => setFlash(error.message));
                      }, 5000);
                    }

                    async function loadOutput() {
                      const root = document.getElementById("outputFiles");
                      const label = document.getElementById("outputFileLabel");
                      const editor = document.getElementById("outputEditor");
                      const summary = document.getElementById("outputSummary");
                      currentOutputFile = "";
                      if (!currentAgent) {
                        root.textContent = "Select an agent to load output files.";
                        label.textContent = "No output file selected";
                        editor.value = "";
                        summary.textContent = "Select an agent to load its output files.";
                        return;
                      }
                      const data = await api("/api/output?id=" + encodeURIComponent(currentAgent));
                      root.innerHTML = "";
                      const files = data.files || [];
                      if (!files.length) {
                        root.textContent = "No output files found in this agent workspace.";
                        label.textContent = "No output file selected";
                        editor.value = "";
                        summary.textContent = "No output files found for this agent.";
                        return;
                      }
                      files.forEach(file => {
                        const button = document.createElement("button");
                        button.className = "tab-button";
                        button.textContent = file.primary ? file.path + " (Primary)" : file.path;
                        button.onclick = () => selectOutputFile(file.path);
                        root.appendChild(button);
                      });
                      await selectOutputFile(data.selectedPath || files[0].path);
                    }

                    async function selectOutputFile(path) {
                      currentOutputFile = path || "";
                      document.querySelectorAll("#outputFiles button").forEach(button => {
                        const base = button.textContent.replace(" (Primary)", "");
                        button.classList.toggle("active", base === currentOutputFile);
                      });
                      const data = await api("/api/output?id=" + encodeURIComponent(currentAgent) + "&path=" + encodeURIComponent(currentOutputFile));
                      const label = document.getElementById("outputFileLabel");
                      const editor = document.getElementById("outputEditor");
                      const summary = document.getElementById("outputSummary");
                      label.textContent = data.selectedPath || "No output file selected";
                      editor.value = data.content || "";
                      const isPrimary = data.primaryPath && data.selectedPath === data.primaryPath;
                      summary.textContent = data.selectedPath
                        ? (isPrimary
                            ? "Primary output file for " + currentAgent + ": " + data.selectedPath
                            : "Viewing output file for " + currentAgent + ": " + data.selectedPath)
                        : "No output file configured for this agent.";
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
                      const runState = document.getElementById("runStateSelect").value;
                      const executionTarget = document.getElementById("executionTargetSelect").value;
                      const runIntervalSeconds = document.getElementById("runIntervalInput").value.trim() || "300";
                      const preferredLocalPool = document.getElementById("preferredLocalPoolSelect").value || "default";
                      const primaryOutputFile = document.getElementById("primaryOutputFileInput").value.trim();
                      await api("/api/agent-settings?id=" + encodeURIComponent(currentAgent), {
                        method: "PUT",
                        body: JSON.stringify({ runState, executionTarget, preferredLocalPool, runIntervalSeconds, primaryOutputFile })
                      });
                      setFlash("Saved agent settings.");
                      const details = await api("/api/agent?id=" + encodeURIComponent(currentAgent));
                      currentAgentDetails = details;
                      document.getElementById("agentStatus").textContent = details.status || "No status.";
                      renderAgentDetails();
                      await loadOutput();
                      await loadAgents();
                    }

                    async function updateRunState(runState) {
                      if (!currentAgent || !currentAgentDetails) {
                        setFlash("Select an agent first.");
                        return;
                      }
                      const executionTarget = currentAgentDetails.executionTarget || document.getElementById("executionTargetSelect").value || "local";
                      const preferredLocalPool = currentAgentDetails.preferredLocalPool || document.getElementById("preferredLocalPoolSelect").value || "default";
                      const runIntervalSeconds = readRunInterval(currentAgentDetails.status || "") || document.getElementById("runIntervalInput").value.trim() || "300";
                      const primaryOutputFile = currentAgentDetails.primaryOutputFile || document.getElementById("primaryOutputFileInput").value.trim() || "";
                      await api("/api/agent-settings?id=" + encodeURIComponent(currentAgent), {
                        method: "PUT",
                        body: JSON.stringify({ runState, executionTarget, preferredLocalPool, runIntervalSeconds, primaryOutputFile })
                      });
                      const details = await api("/api/agent?id=" + encodeURIComponent(currentAgent));
                      currentAgentDetails = details;
                      document.getElementById("agentStatus").textContent = details.status || "No status.";
                      renderAgentDetails();
                      await loadAgents();
                      setFlash(runState === "running" ? "Agent set to running." : "Agent paused.");
                    }

                    async function saveOutputFile() {
                      if (!currentAgent || !currentOutputFile) {
                        setFlash("Select an agent first.");
                        return;
                      }
                      const content = document.getElementById("outputEditor").value;
                      await api("/api/output?id=" + encodeURIComponent(currentAgent) + "&path=" + encodeURIComponent(currentOutputFile), {
                        method: "PUT",
                        body: JSON.stringify({ content })
                      });
                      setFlash("Saved output file.");
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
                      const suffix = currentAgent ? "?agentId=" + encodeURIComponent(currentAgent) : "";
                      const data = await api("/api/usage" + suffix);
                      const series = await api("/api/usage-series" + suffix);
                      const root = document.getElementById("usageBox");
                      const scopeLabel = currentAgent ? ("Agent " + currentAgent) : "All agents";
                      renderUsageChart("usageHourlyChart", "usageHourlyCaption", series.hourly || [], scopeLabel + " | Last 24 hours");
                      renderUsageChart("usageDailyChart", "usageDailyCaption", series.daily || [], scopeLabel + " | Last 30 days");
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
                        persistChatState();
                        setFlash("");
                        loadUsage();
                        loadActivity();
                      } catch (error) {
                        document.getElementById("responseBox").textContent = error.message;
                        persistChatState();
                        setFlash("Request failed.");
                      }
                    }

                    document.querySelectorAll(".tab-button").forEach(button => {
                      button.onclick = () => activateTab(button.dataset.tab);
                    });
                    document.getElementById("send").onclick = sendPrompt;
                    document.getElementById("runAgentButton").onclick = () => updateRunState("running");
                    document.getElementById("pauseAgentButton").onclick = () => updateRunState("paused");
                    document.getElementById("saveSettings").onclick = saveAgentSettings;
                    document.getElementById("saveOutput").onclick = saveOutputFile;
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
