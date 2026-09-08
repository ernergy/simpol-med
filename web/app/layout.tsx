import "./globals.css";

export const metadata = {
  title: "SIMPLE",
  description: "Smart Learning for Medical People",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
