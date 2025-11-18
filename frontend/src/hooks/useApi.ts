import { API_BASE_URL } from "../config";
import type { CheckResponse } from "../types";

export async function checkUrl(url: string): Promise<CheckResponse> {
    const response = await fetch(`${API_BASE_URL}/api/links/check`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({ url }),
    });

    if (!response.ok) {
        throw new Error("Erro ao consultar API");
    }

    return response.json();
}
