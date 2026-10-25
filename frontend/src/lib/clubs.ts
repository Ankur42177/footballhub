/**
 * The 2026-27 Premier League clubs, mirrored from the backend seed (V4 migration) so the
 * club navigation can render in the static shell without a request. Club pages always
 * load live data (followers, posts) from the API.
 *
 * `color` is the club accent used for thin kit stripes (tuned to read on navy).
 * `badge` describes FootballHub's own shield for the club, drawn from its kit colours.
 * These are original designs, not the clubs' crests, which are trademarks that need a licence.
 */
export type BadgePattern = "plain" | "stripes" | "halves" | "sash" | "hoops" | "chevron";

export interface ClubBadge {
  primary: string;
  secondary: string;
  pattern: BadgePattern;
  /** Colour of the initial on the shield. */
  ink: string;
}

export interface ClubNavItem {
  name: string;
  slug: string;
  shortName: string;
  color: string;
  badge: ClubBadge;
}

export const CLUBS: ClubNavItem[] = [
  { name: "Arsenal", slug: "arsenal", shortName: "ARS", color: "#EF0107", badge: { primary: "#EF0107", secondary: "#FFFFFF", pattern: "chevron", ink: "#FFFFFF" } },
  { name: "Aston Villa", slug: "aston-villa", shortName: "AVL", color: "#95BFE5", badge: { primary: "#7A1E46", secondary: "#95BFE5", pattern: "halves", ink: "#FFFFFF" } },
  { name: "Bournemouth", slug: "bournemouth", shortName: "BOU", color: "#DA291C", badge: { primary: "#DA291C", secondary: "#111111", pattern: "stripes", ink: "#FFFFFF" } },
  { name: "Brentford", slug: "brentford", shortName: "BRE", color: "#E30613", badge: { primary: "#E30613", secondary: "#FFFFFF", pattern: "stripes", ink: "#111111" } },
  { name: "Brighton & Hove Albion", slug: "brighton-and-hove-albion", shortName: "BHA", color: "#0057B8", badge: { primary: "#0057B8", secondary: "#FFFFFF", pattern: "stripes", ink: "#0057B8" } },
  { name: "Chelsea", slug: "chelsea", shortName: "CHE", color: "#2B6FD6", badge: { primary: "#034694", secondary: "#FFFFFF", pattern: "plain", ink: "#FFFFFF" } },
  { name: "Coventry City", slug: "coventry-city", shortName: "COV", color: "#59CBE8", badge: { primary: "#59CBE8", secondary: "#FFFFFF", pattern: "sash", ink: "#0B3B5C" } },
  { name: "Crystal Palace", slug: "crystal-palace", shortName: "CRY", color: "#3D6FD8", badge: { primary: "#1B458F", secondary: "#C4122E", pattern: "halves", ink: "#FFFFFF" } },
  { name: "Everton", slug: "everton", shortName: "EVE", color: "#2A5BD7", badge: { primary: "#003399", secondary: "#FFFFFF", pattern: "chevron", ink: "#FFFFFF" } },
  { name: "Fulham", slug: "fulham", shortName: "FUL", color: "#E8E8E8", badge: { primary: "#FFFFFF", secondary: "#111111", pattern: "chevron", ink: "#111111" } },
  { name: "Hull City", slug: "hull-city", shortName: "HUL", color: "#F5A12D", badge: { primary: "#F5A12D", secondary: "#111111", pattern: "stripes", ink: "#111111" } },
  { name: "Ipswich Town", slug: "ipswich-town", shortName: "IPS", color: "#4A7BD0", badge: { primary: "#3A64A3", secondary: "#FFFFFF", pattern: "hoops", ink: "#FFFFFF" } },
  { name: "Leeds United", slug: "leeds-united", shortName: "LEE", color: "#FFCD00", badge: { primary: "#FFFFFF", secondary: "#FFCD00", pattern: "sash", ink: "#1D428A" } },
  { name: "Liverpool", slug: "liverpool", shortName: "LIV", color: "#C8102E", badge: { primary: "#C8102E", secondary: "#F6EB61", pattern: "plain", ink: "#FFFFFF" } },
  { name: "Manchester City", slug: "manchester-city", shortName: "MCI", color: "#6CABDD", badge: { primary: "#6CABDD", secondary: "#1C2C5B", pattern: "chevron", ink: "#1C2C5B" } },
  { name: "Manchester United", slug: "manchester-united", shortName: "MUN", color: "#DA291C", badge: { primary: "#DA291C", secondary: "#FBE122", pattern: "hoops", ink: "#FFFFFF" } },
  { name: "Newcastle United", slug: "newcastle-united", shortName: "NEW", color: "#F2F2F2", badge: { primary: "#111111", secondary: "#FFFFFF", pattern: "stripes", ink: "#111111" } },
  { name: "Nottingham Forest", slug: "nottingham-forest", shortName: "NFO", color: "#DD0000", badge: { primary: "#DD0000", secondary: "#FFFFFF", pattern: "sash", ink: "#FFFFFF" } },
  { name: "Sunderland", slug: "sunderland", shortName: "SUN", color: "#EB172B", badge: { primary: "#EB172B", secondary: "#FFFFFF", pattern: "stripes", ink: "#111111" } },
  { name: "Tottenham Hotspur", slug: "tottenham-hotspur", shortName: "TOT", color: "#E8E8F0", badge: { primary: "#FFFFFF", secondary: "#132257", pattern: "plain", ink: "#132257" } },
];

export function clubColor(slug: string | null | undefined, fallback?: string | null): string {
  return CLUBS.find((club) => club.slug === slug)?.color ?? fallback ?? "#34507A";
}

export function findClub(slug: string | null | undefined): ClubNavItem | undefined {
  return CLUBS.find((club) => club.slug === slug);
}

/** Colour for tinted backgrounds: the badge's main colour, or its second colour when the main one is white or black. */
export function clubTint(slug: string | null | undefined): string {
  const club = findClub(slug);
  if (!club) return "#34507A";
  const isNeutral = (hex: string) => ["#FFFFFF", "#111111"].includes(hex.toUpperCase());
  const { primary, secondary } = club.badge;
  if (!isNeutral(primary)) return primary;
  return isNeutral(secondary) ? club.color : secondary;
}
