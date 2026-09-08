import "./globals.css";

export const metadata = {
  title: "SIMPLE Control",
  description: "Panel real de costos y consumo de SIMPLE",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="es">
      <body>{children}</body>
    </html>
  );
}
