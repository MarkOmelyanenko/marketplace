const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

function getOpsToken() {
  return localStorage.getItem("opsToken") || "ops-dev";
}

function setOpsToken(token) {
  localStorage.setItem("opsToken", token);
}

function getHeaders() {
  return {
    "Content-Type": "application/json",
    "Ops-Token": getOpsToken(),
  };
}

async function handleResponse(response) {
  if (response.status === 401) {
    throw new Error("UNAUTHORIZED");
  }
  if (!response.ok) {
    const error = await response
      .json()
      .catch(() => ({ message: response.statusText }));
    throw new Error(error.message || `HTTP error! status: ${response.status}`);
  }
  return response.json();
}

export function shortenUuid(uuid) {
  if (!uuid) return "";
  return uuid.substring(0, 8) + "...";
}

export async function opsSearchPayments(params) {
  const queryParams = new URLSearchParams();
  if (params.paymentId) queryParams.append("paymentId", params.paymentId);
  if (params.offerId) queryParams.append("offerId", params.offerId);
  if (params.partnerId) queryParams.append("partnerId", params.partnerId);
  if (params.createdFrom) queryParams.append("createdFrom", params.createdFrom);
  if (params.createdTo) queryParams.append("createdTo", params.createdTo);
  if (params.status) queryParams.append("status", params.status);

  const response = await fetch(
    `${API_BASE_URL}/api/ops/payments/search?${queryParams}`,
    {
      method: "GET",
      headers: getHeaders(),
    },
  );
  return handleResponse(response);
}

export async function opsGetPayment(id) {
  const response = await fetch(`${API_BASE_URL}/api/ops/payments/${id}`, {
    method: "GET",
    headers: getHeaders(),
  });
  return handleResponse(response);
}

export async function opsGetTimeline(id) {
  const response = await fetch(
    `${API_BASE_URL}/api/ops/payments/${id}/timeline`,
    {
      method: "GET",
      headers: getHeaders(),
    },
  );
  return handleResponse(response);
}

export async function opsRetryLatestWebhook(id) {
  const response = await fetch(
    `${API_BASE_URL}/api/ops/payments/${id}/retry-latest-webhook`,
    {
      method: "POST",
      headers: getHeaders(),
    },
  );
  return handleResponse(response);
}

export async function opsListRefundRequests(status) {
  const url = status
    ? `${API_BASE_URL}/api/ops/refund-requests?status=${encodeURIComponent(status)}`
    : `${API_BASE_URL}/api/ops/refund-requests`;
  const response = await fetch(url, {
    method: "GET",
    headers: getHeaders(),
  });
  return handleResponse(response);
}

export async function opsApproveRefundRequest(id) {
  const response = await fetch(
    `${API_BASE_URL}/api/ops/refund-requests/${id}/approve`,
    {
      method: "PATCH",
      headers: getHeaders(),
    },
  );
  return handleResponse(response);
}

export async function opsRejectRefundRequest(id, rejectionReason) {
  const response = await fetch(
    `${API_BASE_URL}/api/ops/refund-requests/${id}/reject`,
    {
      method: "PATCH",
      headers: getHeaders(),
      body: JSON.stringify({ rejectionReason: rejectionReason || "" }),
    },
  );
  return handleResponse(response);
}

export { getOpsToken, setOpsToken };
