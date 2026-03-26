import { GoogleGenAI } from "@google/genai";

export const generateProfileImage = async (base64Image: string, apiKey: string) => {
  const ai = new GoogleGenAI({ apiKey });
  const response = await ai.models.generateContent({
    model: 'gemini-2.5-flash-image',
    contents: {
      parts: [
        {
          inlineData: {
            data: base64Image,
            mimeType: "image/png",
          },
        },
        {
          text: "Generate a high-quality, realistic, normal portrait photo of this person (Leon S. Kennedy). Keep his facial features, hair style, and appearance exactly the same as in the image. Change the lighting to be natural and clear, and the setting to a normal, everyday environment. The final result should look like a real human photograph, not a video game screenshot. High resolution, detailed skin texture, cinematic but natural lighting.",
        },
      ],
    },
  });

  for (const part of response.candidates[0].content.parts) {
    if (part.inlineData) {
      return `data:image/png;base64,${part.inlineData.data}`;
    }
  }
  return null;
};
