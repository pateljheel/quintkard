import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Quintkard",
  description: "Slim operational interface for Quintkard"
};

export default function RootLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
