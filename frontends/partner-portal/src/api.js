// Use same-origin /api when on HTTPS (tunnel) or under /partner/ to avoid Mixed Content
function getApiBaseUrl() {
  const build = import.meta.env.VITE_API_BASE_URL !== undefined
    ? import.meta.env.VITE_API_BASE_URL
    : "http://localhost:8080";
  if (typeof window !== "undefined") {
    if (window.location.protocol === "https:") return "";
    if (/^\/(partner|buyer|ops)(\/|$)/.test(window.location.pathname)) return "";
  }
  return build;
}
const API_BASE_URL = getApiBaseUrl();

function getPartnerId() {
  return localStorage.getItem("partnerId");
}

function getHeaders() {
  const headers = {
    "Content-Type": "application/json",
  };
  const partnerId = getPartnerId();
  if (partnerId) {
    headers["Partner-Id"] = partnerId;
  }
  return headers;
}

async function handleResponse(response) {
  if (!response.ok) {
    const error = await response
      .json()
      .catch(() => ({ message: response.statusText }));
    throw new Error(error.message || `HTTP error! status: ${response.status}`);
  }
  if (response.status === 204) return null;
  return response.json();
}

export async function listOffers() {
  const response = await fetch(
    `${API_BASE_URL}/api/offers/v1/offers?mine=true`,
    {
      method: "GET",
      headers: getHeaders(),
    },
  );
  return handleResponse(response);
}

export async function createOffer(
  title,
  description,
  priceCents = 499,
  currency = "USD",
  idempotencyKey = null,
) {
  const headers = getHeaders();
  if (idempotencyKey) {
    headers["Idempotency-Key"] = idempotencyKey;
  }
  const response = await fetch(`${API_BASE_URL}/api/offers/v1/offers`, {
    method: "POST",
    headers,
    body: JSON.stringify({ title, description, priceCents, currency }),
  });
  return handleResponse(response);
}

export async function getOffer(id) {
  const response = await fetch(`${API_BASE_URL}/api/offers/v1/offers/${id}`, {
    method: "GET",
    headers: getHeaders(),
  });
  return handleResponse(response);
}

export async function updateOffer(
  id,
  title,
  description,
  priceCents,
  currency,
) {
  const body = {};
  if (title !== undefined) body.title = title;
  if (description !== undefined) body.description = description;
  if (priceCents !== undefined) body.priceCents = priceCents;
  if (currency !== undefined) body.currency = currency;
  const response = await fetch(`${API_BASE_URL}/api/offers/v1/offers/${id}`, {
    method: "PATCH",
    headers: getHeaders(),
    body: JSON.stringify(body),
  });
  return handleResponse(response);
}

export async function applyAi(id, useTitle, useDescription) {
  const response = await fetch(
    `${API_BASE_URL}/api/offers/v1/offers/${id}/apply-ai`,
    {
      method: "POST",
      headers: getHeaders(),
      body: JSON.stringify({ useTitle, useDescription }),
    },
  );
  return handleResponse(response);
}

export async function publishOffer(id) {
  const response = await fetch(
    `${API_BASE_URL}/api/offers/v1/offers/${id}/publish`,
    {
      method: "POST",
      headers: getHeaders(),
    },
  );
  return handleResponse(response);
}

export async function deleteOffer(id) {
  const response = await fetch(`${API_BASE_URL}/api/offers/v1/offers/${id}`, {
    method: "DELETE",
    headers: getHeaders(),
  });
  return handleResponse(response);
}

export async function getWalletBalance() {
  const response = await fetch(
    `${API_BASE_URL}/api/payments/v1/wallet/balance`,
    {
      method: "GET",
      headers: getHeaders(),
    },
  );
  return handleResponse(response);
}

export async function listMyPayments() {
  const response = await fetch(`${API_BASE_URL}/api/payments/v1/payments`, {
    method: "GET",
    headers: getHeaders(),
  });
  return handleResponse(response);
}

export async function deposit(amountCents, currency = "USD") {
  const response = await fetch(
    `${API_BASE_URL}/api/payments/v1/wallet/deposit`,
    {
      method: "POST",
      headers: getHeaders(),
      body: JSON.stringify({ amountCents, currency }),
    },
  );
  return handleResponse(response);
}
