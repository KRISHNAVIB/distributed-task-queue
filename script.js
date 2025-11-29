document.addEventListener("DOMContentLoaded", () => {

    console.log("Frontend loaded.");

    const scrollBtn = document.getElementById("scrollToFormBtn");
    const refreshBtn = document.getElementById("refreshBtn");
    const submitBtn = document.getElementById("submitJobBtn");

    const tenantIdInput = document.getElementById("tenantId");
    const idempotencyKeyInput = document.getElementById("idempotencyKey");
    const payloadInput = document.getElementById("payload");
    const maxRetriesInput = document.getElementById("maxRetries");

    submitBtn.addEventListener("click", async () => {

        console.log("Submit Job clicked.");

        const tenantId = tenantIdInput.value.trim();
        const payloadText = payloadInput.value.trim();
        const maxRetries = parseInt(maxRetriesInput.value.trim());
        const idempotencyKey = idempotencyKeyInput.value.trim();

        if (!tenantId) {
            alert("Tenant ID is required.");
            return;
        }

        let payloadObj = null;

        try {
            const cleaned = payloadText
                .replace(/“|”/g, '"')
                .replace(/‘|’/g, "'")
                .replace(/：/g, ":");

            payloadObj = JSON.parse(cleaned);
        } catch (err) {
            alert('Invalid JSON payload.\nExample: {"forceFail": true}');
            return;
        }

        const body = {
            tenantId: tenantId,
            payloadJson: JSON.stringify(payloadObj),
            maxRetries: maxRetries
        };

        const headers = { "Content-Type": "application/json" };
        if (idempotencyKey !== "") {
            headers["Idempotency-Key"] = idempotencyKey;
        }

        try {
            const response = await fetch(
                "https://distributed-task-queue-production.up.railway.app/jobs",
                {
                    method: "POST",
                    headers: headers,
                    body: JSON.stringify(body)
                }
            );

            if (!response.ok) {
                alert("Backend error: " + response.status);
                return;
            }

            const result = await response.json();
            console.log("Job submitted:", result);

            alert("Job submitted successfully!\nJob ID: " + result.jobId);
            maxRetriesInput.value = "3";

        } catch (error) {
            console.error("Error submitting job:", error);
            alert("Failed to reach backend. Check connectivity.");
        }
    });

    async function refreshDashboard() {
        console.log("Refreshing dashboard...");

        try {
            const pending = await (await fetch("https://distributed-task-queue-production.up.railway.app/jobs/pending")).json();
            document.getElementById("pendingList").innerHTML =
                Array.isArray(pending)
                    ? pending.map(job => `<div class='job-item'>${job}</div>`).join("")
                    : `<div class='job-item'>INVALID DATA: ${JSON.stringify(pending)}</div>`;

            const running = await (await fetch("https://distributed-task-queue-production.up.railway.app/jobs/running")).json();
            document.getElementById("runningList").innerHTML =
                Array.isArray(running)
                    ? running.map(job => `<div class='job-item'>${job}</div>`).join("")
                    : `<div class='job-item'>INVALID DATA: ${JSON.stringify(running)}</div>`;

            const completed = await (await fetch("https://distributed-task-queue-production.up.railway.app/jobs/completed")).json();
            document.getElementById("completedList").innerHTML =
                Array.isArray(completed)
                    ? completed.map(job => `<div class='job-item'>${job}</div>`).join("")
                    : `<div class='job-item'>INVALID DATA: ${JSON.stringify(completed)}</div>`;

            const dlq = await (await fetch("https://distributed-task-queue-production.up.railway.app/jobs/dlq")).json();
            document.getElementById("dlqList").innerHTML =
                Array.isArray(dlq)
                    ? dlq.map(job => `<div class='job-item'>${job}</div>`).join("")
                    : `<div class='job-item'>INVALID DATA: ${JSON.stringify(dlq)}</div>`;

            const metrics = await (await fetch("https://distributed-task-queue-production.up.railway.app/jobs/metrics")).json();

            document.getElementById("m_submitted").innerText = metrics.submitted;
            document.getElementById("m_completed").innerText = metrics.completed;
            document.getElementById("m_failed").innerText = metrics.failed;
            document.getElementById("m_retried").innerText = metrics.retried;

        } catch (error) {
            console.error("Dashboard refresh failed:", error);
        }
    }

    refreshBtn.addEventListener("click", refreshDashboard);

    setInterval(refreshDashboard, 2000);

    refreshDashboard();
});
