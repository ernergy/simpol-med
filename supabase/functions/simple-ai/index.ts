import "jsr:@supabase/functions-js/edge-runtime.d.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const apiKey = Deno.env.get("OPEN_AI_KEY");
    if (!apiKey) throw new Error("OPEN_AI_KEY no está configurada");

    const body = await req.json();
    const prompt = String(body?.prompt ?? "").trim();
    const bookName = String(body?.bookName ?? "").trim();

    if (!prompt) {
      return new Response(JSON.stringify({ error: "Escribe una solicitud" }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const context = bookName
      ? `El usuario seleccionó un archivo llamado "${bookName}". Aún no recibiste el contenido del PDF. No inventes contenido del libro.`
      : "No hay un libro adjunto en esta llamada.";

    const response = await fetch("https://api.openai.com/v1/responses", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${apiKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        model: "gpt-5.6-luna",
        reasoning: { effort: "low" },
        max_output_tokens: 1800,
        instructions:
          `Eres SIMPLE, un asistente de estudio para estudiantes y profesionales de medicina. Responde siempre en español claro, estructurado y útil para estudiar. Puedes explicar, resumir, comparar conceptos y preparar contenido para presentaciones. No inventes información. ${context}`,
        input: prompt,
      }),
    });

    const data = await response.json();

    if (!response.ok) {
      return new Response(
        JSON.stringify({
          error: data?.error?.message ?? "No se pudo consultar la IA",
        }),
        {
          status: response.status,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        },
      );
    }

    let answer = "";
    for (const item of data?.output ?? []) {
      if (item?.type === "message") {
        for (const content of item?.content ?? []) {
          if (content?.type === "output_text") {
            answer += content?.text ?? "";
          }
        }
      }
    }

    return new Response(
      JSON.stringify({
        answer: answer || "La IA respondió sin texto visible.",
        model: data?.model ?? "gpt-5.6-luna",
        usage: data?.usage ?? null,
      }),
      {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      },
    );
  } catch (error) {
    return new Response(
      JSON.stringify({
        error: error instanceof Error ? error.message : "Error inesperado",
      }),
      {
        status: 500,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      },
    );
  }
});
