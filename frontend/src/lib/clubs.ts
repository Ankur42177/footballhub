/**
 * The 2026-27 Premier League clubs, mirrored from the backend seed (V4 migration) so the
 * club navigation can render in the static shell without a request. Club pages always
 * load live data (followers, posts) from the API.
 */
export interface ClubNavItem {
  name: string;
  slug: string;
  shortName: string;
  color: string;
}

export const CLUBS: ClubNavItem[] = [
  { name: "Arsenal", slug: "arsenal", shortName: "ARS", color: "#EF0107" },
  { name: "Aston Villa", slug: "aston-villa", shortName: "AVL", color: "#95BFE5" },
  { name: "Bournemouth", slug: "bournemouth", shortName: "BOU", color: "#DA291C" },
  { name: "Brentford", slug: "brentford", shortName: "BRE", color: "#E30613" },
  { name: "Brighton & Hove Albion", slug: "brighton-and-hove-albion", shortName: "BHA", color: "#0057B8" },
  { name: "Chelsea", slug: "chelsea", shortName: "CHE", color: "#2B6FD6" },
  { name: "Coventry City", slug: "coventry-city", shortName: "COV", color: "#59CBE8" },
  { name: "Crystal Palace", slug: "crystal-palace", shortName: "CRY", color: "#3D6FD8" },
  { name: "Everton", slug: "everton", shortName: "EVE", color: "#2A5BD7" },
  { name: "Fulham", slug: "fulham", shortName: "FUL", color: "#E8E8E8" },
  { name: "Hull City", slug: "hull-city", shortName: "HUL", color: "#F5A12D" },
  { name: "Ipswich Town", slug: "ipswich-town", shortName: "IPS", color: "#4A7BD0" },
  { name: "Leeds United", slug: "leeds-united", shortName: "LEE", color: "#FFCD00" },
  { name: "Liverpool", slug: "liverpool", shortName: "LIV", color: "#C8102E" },
  { name: "Manchester City", slug: "manchester-city", shortName: "MCI", color: "#6CABDD" },
  { name: "Manchester United", slug: "manchester-united", shortName: "MUN", color: "#DA291C" },
  { name: "Newcastle United", slug: "newcastle-united", shortName: "NEW", color: "#F2F2F2" },
  { name: "Nottingham Forest", slug: "nottingham-forest", shortName: "NFO", color: "#DD0000" },
  { name: "Sunderland", slug: "sunderland", shortName: "SUN", color: "#EB172B" },
  { name: "Tottenham Hotspur", slug: "tottenham-hotspur", shortName: "TOT", color: "#E8E8F0" },
];

export function clubColor(slug: string | null | undefined, fallback?: string | null): string {
  return CLUBS.find((club) => club.slug === slug)?.color ?? fallback ?? "#34507A";
}
