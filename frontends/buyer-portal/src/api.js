// Use same-origin /api when on HTTPS (tunnel) or under /buyer/ to avoid Mixed Content
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

function getBuyerId() {
  return localStorage.getItem("buyerId");
}

function setBuyerId(buyerId) {
  localStorage.setItem("buyerId", buyerId);
}

function getHeaders() {
  const headers = {
    "Content-Type": "application/json",
  };

  const buyerId = getBuyerId();
  if (buyerId) {
    headers["Buyer-Id"] = buyerId;
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
  return response.json();
}

export async function listCatalogOffers() {
  const response = await fetch(
    `${API_BASE_URL}/api/catalog/v1/catalog/offers`,
    {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
    },
  );
  return handleResponse(response);
}

export async function createOrder(offerId, quantity = 1) {
  const response = await fetch(`${API_BASE_URL}/api/orders/v1/orders`, {
    method: "POST",
    headers: getHeaders(),
    body: JSON.stringify({ offerId, quantity: quantity >= 1 ? quantity : 1 }),
  });
  return handleResponse(response);
}

export async function listMyOrders() {
  const response = await fetch(
    `${API_BASE_URL}/api/orders/v1/orders?mine=true`,
    {
      method: "GET",
      headers: getHeaders(),
    },
  );
  return handleResponse(response);
}

export async function getOrder(id) {
  const response = await fetch(`${API_BASE_URL}/api/orders/v1/orders/${id}`, {
    method: "GET",
    headers: getHeaders(),
  });
  return handleResponse(response);
}

export async function retryOrderPayment(orderId) {
  const response = await fetch(`${API_BASE_URL}/api/orders/v1/orders/${orderId}/retry-payment`, {
    method: "POST",
    headers: getHeaders(),
  });
  return handleResponse(response);
}

export async function cancelOrder(orderId) {
  const response = await fetch(`${API_BASE_URL}/api/orders/v1/orders/${orderId}/cancel`, {
    method: "POST",
    headers: getHeaders(),
  });
  return handleResponse(response);
}

export async function createRefundRequest(orderId, { reason, details }) {
  const response = await fetch(
    `${API_BASE_URL}/api/orders/v1/orders/${orderId}/refund-request`,
    {
      method: "POST",
      headers: getHeaders(),
      body: JSON.stringify({ reason, details: details || "" }),
    }
  );
  return handleResponse(response);
}

export async function getWalletBalance() {
  const response = await fetch(`${API_BASE_URL}/api/payments/v1/wallet/balance`, {
    method: "GET",
    headers: getHeaders(),
  });
  return handleResponse(response);
}

export async function deposit(amountCents, currency = "USD") {
  const response = await fetch(`${API_BASE_URL}/api/payments/v1/wallet/deposit`, {
    method: "POST",
    headers: getHeaders(),
    body: JSON.stringify({ amountCents, currency }),
  });
  return handleResponse(response);
}

export { getBuyerId, setBuyerId };
