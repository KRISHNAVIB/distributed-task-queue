// script.js
// =======================
// STEP: Full dashboard logic (Submit + Refresh + Idempotency)
// =======================

document.addEventListener("DOMContentLoaded", () => {

    console.log("JS Loaded Successfully!");

    // Buttons
    const scrollBtn = document.getElementById("scrollToFormBtn");
    const refreshBtn = document.getElementById("refreshBtn");
    const submitBtn = document.getElementById("submitJobBtn");

    // Form inputs
    const tenantIdInput = document.getElementById("tenantId");
    const idempotencyKeyInput = document.getElementById("idempotencyKey");
    const payloadInput = document.getElementById("payload");
    const maxRetriesInput = document.getElementById("maxRetries");



    // =======================
    // SUBMIT JOB
    // =======================
    submitBtn.addEventListener("click", async () => {

        console.log("Submit Job clicked!");

        const tenantId = tenantIdInput.value.trim();
        const payloadText = payloadInput.value.trim();
        const maxRetries = parseInt(maxRetriesInput.value.trim());
        const idemKey = idempotencyKeyInput.value.trim();

        if (!tenantId) {
            alert("Tenant ID is required");
            return;
        }

        // Validate JSON
        let payloadObj = null;
        try {
            payloadObj = JSON.parse(payloadText);
        } catch (err) {
            alert("Invalid JSON payload.\nExample: {\"forceFail\": true}");
            return;
        }

        // Prepare request body
        const body = {
            tenantId: tenantId,
            payloadJson: JSON.stringify(payloadObj),
            maxRetries: maxRetries
        };

        // Prepare headers
        const headers = { "Content-Type": "application/json" };
        if (idemKey !== "") {
            headers["Idempotency-Key"] = idemKey;
        }

        try {
            const response = await fetch("http://localhost:8080/jobs", {
                method: "POST",
                headers: headers,
                body: JSON.stringify(body)
            });

            if (!response.ok) {
                alert("Backend error: " + response.status);
                return;
            }

            const result = await response.json();
            console.log("Job submitted:", result);

            alert("Job submitted successfully!\nJob ID: " + result.jobId);

            // Optional: reset only retries
            maxRetriesInput.value = "3";

        } catch (error) {
            console.error("Error submitting job:", error);
            alert("Failed to reach backend. Is it running?");
        }

    });

    // =======================
    // DASHBOARD AUTO-REFRESH
    // =======================
    async function refreshDashboard() {
    console.log("Refreshing dashboard...");

    try {
        // Pending
        const pendingResp = await fetch("http://localhost:8080/jobs/pending");
        console.log("PENDING RAW RESPONSE:", pendingResp);

        const pending = await pendingResp.json();
        console.log("PENDING JSON =", pending);

        // Render pending
        document.getElementById("pendingList").innerHTML =
            Array.isArray(pending)
                ? pending.map(job => `<div class='job-item'>${job}</div>`).join("")
                : `<div class='job-item'>INVALID DATA: ${JSON.stringify(pending)}</div>`;


        // Running
        const runningResp = await fetch("http://localhost:8080/jobs/running");
        console.log("RUNNING RAW RESPONSE:", runningResp);

        const running = await runningResp.json();
        console.log("RUNNING JSON =", running);

        document.getElementById("runningList").innerHTML =
            Array.isArray(running)
                ? running.map(job => `<div class='job-item'>${job}</div>`).join("")
                : `<div class='job-item'>INVALID DATA: ${JSON.stringify(running)}</div>`;


        // Completed
        const completedResp = await fetch("http://localhost:8080/jobs/completed");
        console.log("COMPLETED RAW RESPONSE:", completedResp);

        const completed = await completedResp.json();
        console.log("COMPLETED JSON =", completed);

        document.getElementById("completedList").innerHTML =
            Array.isArray(completed)
                ? completed.map(job => `<div class='job-item'>${job}</div>`).join("")
                : `<div class='job-item'>INVALID DATA: ${JSON.stringify(completed)}</div>`;


        // DLQ
        const dlqResp = await fetch("http://localhost:8080/jobs/dlq");
        console.log("DLQ RAW RESPONSE:", dlqResp);

        const dlq = await dlqResp.json();
        console.log("DLQ JSON =", dlq);

        document.getElementById("dlqList").innerHTML =
            Array.isArray(dlq)
                ? dlq.map(job => `<div class='job-item'>${job}</div>`).join("")
                : `<div class='job-item'>INVALID DATA: ${JSON.stringify(dlq)}</div>`;


        // Metrics
        const metricsResp = await fetch("http://localhost:8080/jobs/metrics");
        console.log("METRICS RAW RESPONSE:", metricsResp);

        const metrics = await metricsResp.json();
        console.log("METRICS JSON =", metrics);

        document.getElementById("m_submitted").innerText = metrics.submitted;
        document.getElementById("m_completed").innerText = metrics.completed;
        document.getElementById("m_failed").innerText = metrics.failed;
        document.getElementById("m_retried").innerText = metrics.retried;

    } catch (error) {
        console.error("Dashboard refresh failed:", error);
    }
}

    // Manual refresh button
    refreshBtn.addEventListener("click", refreshDashboard);

    // Auto refresh every 2 seconds
    setInterval(refreshDashboard, 2000);

    // Initial load
    refreshDashboard();
});
