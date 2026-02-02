<!DOCTYPE html>
<html lang="en">
<body>

<h1>GitRelease 🚀</h1>
<p><strong>Your private Play Store for QA, built on top of GitHub Releases.</strong></p>

<p>
GitRelease exists to solve one painfully common problem:
<br />
<strong>developers manually sharing APKs over Slack, WhatsApp, Drive, or email for testing.</strong>
</p>

<p>
This app removes that entire workflow.
</p>

<p>
GitRelease fetches release data directly from GitHub repositories, shows all
<strong>Release</strong> and <strong>Pre-release</strong> APKs, and lets testers
<strong>download and install builds instantly</strong> — straight from GitHub.
</p>

<p>
Think of it as a <strong>secondary Play Store</strong>, but
<strong>only for QA and internal testing</strong>.
</p>

<hr />

<h2>What Problem Does This Solve?</h2>
<ul>
  <li>No more APKs shared in chat threads</li>
  <li>No more “latest build?” messages</li>
  <li>No more guessing which APK is correct</li>
  <li>No more manual distribution for testers</li>
</ul>

<p>
If the APK is attached to a GitHub Release →
<strong>GitRelease can fetch and install it</strong>.
</p>

<hr />

<h2>Core Features</h2>

<h3>📦 GitHub Releases as a Distribution Channel</h3>
<ul>
  <li>Fetches <strong>repository details</strong> directly from GitHub</li>
  <li>Lists <strong>Release</strong> and <strong>Pre-release</strong> tracks separately</li>
  <li>Displays all APK assets attached to a release</li>
  <li>One-tap download and install</li>
</ul>

<h3>🧪 Built for QA &amp; Internal Testing</h3>
<ul>
  <li>Perfect for:
    <ul>
      <li>QA teams</li>
      <li>Internal testers</li>
      <li>Dogfooding builds</li>
    </ul>
  </li>
  <li>Acts like a <strong>private Play Store</strong>, without Play Store delays or approvals</li>
</ul>

<h3>🔐 Public &amp; Private Repository Support</h3>
<ul>
  <li>Works with <strong>public repositories</strong> out of the box</li>
  <li>Supports <strong>private repositories</strong> using a
    <strong>GitHub Personal Access Token</strong>
  </li>
  <li>Token scope required: <strong>repo</strong></li>
  <li>Token is used <strong>only</strong> to fetch release metadata</li>
  <li>No token is stored remotely</li>
</ul>

<h3>🔄 No Manual Sharing, Ever Again</h3>
<ul>
  <li>Developers push a release → testers see it instantly</li>
  <li>No Slack uploads</li>
  <li>No WhatsApp APK chaos</li>
  <li>No Drive links expiring</li>
</ul>

<hr />

<h2>Safety, Privacy &amp; Transparency (No Fine Print)</h2>

<p><strong>Let’s be very clear:</strong></p>
<ul>
  <li>✅ No Firebase</li>
  <li>✅ No analytics</li>
  <li>✅ No logging</li>
  <li>✅ No tracking</li>
  <li>✅ No background uploads</li>
  <li>✅ No hidden API calls</li>
</ul>

<p>The app:</p>
<ul>
  <li>Talks <strong>only</strong> to GitHub’s official APIs</li>
  <li>Makes <strong>read-only</strong> requests</li>
  <li>Does <strong>not</strong> collect, store, or transmit user data</li>
</ul>

<p>
This project is <strong>100% open source</strong>.
<br />
There is <strong>nothing to hide</strong> — you can audit every line of code yourself.
</p>

<hr />

<h2>Open Source by Design 🧩</h2>

<p>
GitRelease is fully open source so teams can:
</p>
<ul>
  <li>Trust what the app does</li>
  <li>Fork it for internal use</li>
  <li>Modify it for company-specific workflows</li>
  <li>Self-host if needed</li>
</ul>

<p>
If something looks suspicious, it isn’t — you can verify it in the code.
</p>

<hr />

<h2>How It Works (High Level)</h2>
<ol>
  <li>Enter a GitHub repository (<code>owner/repo</code>)</li>
  <li>(Optional) Add a GitHub Personal Access Token for private repos</li>
  <li>App fetches:
    <ul>
      <li>Repository metadata</li>
      <li>Releases &amp; pre-releases</li>
      <li>APK assets</li>
    </ul>
  </li>
  <li>Select a build → download → install</li>
</ol>

<hr />

<h2>Demo &amp; Screenshots</h2>

<h3>🎥 Video Demo</h3>
<p>
<strong>[PLACEHOLDER – Demo video coming soon]</strong>
</p>

<!-- Example:
<video src="demo.mp4" controls width="600"></video>
-->

<h3>📱 Screenshots</h3>

<p><strong>[PLACEHOLDER – Screenshot 1]</strong></p>
<!-- <img src="screenshot1.png" width="300" /> -->

<p><strong>[PLACEHOLDER – Screenshot 2]</strong></p>
<!-- <img src="screenshot2.png" width="300" /> -->

<hr />

<h2>Who Is This For?</h2>
<ul>
  <li>Android developers tired of manual APK sharing</li>
  <li>QA teams that need fast access to builds</li>
  <li>Startups without Play Store internal testing pipelines</li>
  <li>Anyone using GitHub Releases as their source of truth</li>
</ul>

<hr />

<h2>TL;DR</h2>
<ul>
  <li>GitHub Releases → APK distribution</li>
  <li>Zero tracking</li>
  <li>Zero data collection</li>
  <li>Fully open source</li>
  <li>Built specifically for QA</li>
</ul>

<p>
<strong>Ship once. Test everywhere. Stop sharing APKs manually.</strong>
</p>

</body>
</html>
