import type { Metadata, Viewport } from "next";

import "@fontsource-variable/big-shoulders-display";
import "@fontsource-variable/literata";
import "./globals.css";

import { SiteFooter } from "@/components/layout/SiteFooter";
import { SiteHeader } from "@/components/layout/SiteHeader";
import { ClubNavigation } from "@/components/navigation/ClubNavigation";
import { SessionProvider } from "@/components/session/SessionProvider";
import { siteUrl } from "@/lib/format";

export const metadata: Metadata = {
  metadataBase: new URL(siteUrl()),
  title: {
    default: "FootballHub: Premier League analysis by fans",
    template: "%s | FootballHub",
  },
  description:
    "Fan-written Premier League analysis, tactics and opinion. Read, upvote and discuss articles about every club.",
  openGraph: {
    siteName: "FootballHub",
    type: "website",
    locale: "en_GB",
  },
  alternates: { canonical: "/" },
};

export const viewport: Viewport = {
  themeColor: "#0e1a2b",
  colorScheme: "dark",
};

// Keep this layout free of request data (cookies, params): it is part of the static shell.
export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en-GB">
      <body className="flex min-h-screen flex-col">
        <a
          href="#main"
          className="sr-only z-50 rounded-panel bg-floodlight px-4 py-2 font-display text-lg font-bold text-pitch focus:not-sr-only focus:fixed focus:left-3 focus:top-3"
        >
          Skip to content
        </a>
        <SessionProvider>
          <SiteHeader />
          <ClubNavigation />
          <main id="main" tabIndex={-1} className="flex-1 focus:outline-none">
            {children}
          </main>
          <SiteFooter />
        </SessionProvider>
      </body>
    </html>
  );
}
