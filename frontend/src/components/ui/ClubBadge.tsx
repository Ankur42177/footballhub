import { useId } from "react";

import { findClub } from "@/lib/clubs";
import type { ClubBadge as Badge } from "@/lib/clubs";

const SHIELD = "M16 1.5 L29 5.5 V17 C29 26 23 32.5 16 36.5 C9 32.5 3 26 3 17 V5.5 Z";

/** Kit-pattern layers drawn inside the shield (viewBox 32 x 38). */
function Pattern({ badge }: { badge: Badge }) {
  const { secondary } = badge;
  switch (badge.pattern) {
    case "stripes":
      return (
        <g fill={secondary}>
          <rect x="6" y="0" width="4" height="38" />
          <rect x="14" y="0" width="4" height="38" />
          <rect x="22" y="0" width="4" height="38" />
        </g>
      );
    case "halves":
      return <rect x="16" y="0" width="16" height="38" fill={secondary} />;
    case "sash":
      return <polygon points="0,6 8,0 32,30 32,38 26,38" fill={secondary} />;
    case "hoops":
      return (
        <g fill={secondary}>
          <rect x="0" y="11" width="32" height="4" />
          <rect x="0" y="21" width="32" height="4" />
        </g>
      );
    case "chevron":
      return <polygon points="3,5.5 16,14 29,5.5 29,10 16,18.5 3,10" fill={secondary} />;
    default:
      return <rect x="3" y="27" width="26" height="3" fill={secondary} />;
  }
}

/**
 * FootballHub's own shield for a club: kit colours and pattern plus the club's initial.
 * Deliberately not the official crest (those are licensed trademarks).
 */
export function ClubBadge({ slug, size = 20, className = "" }: { slug: string; size?: number; className?: string }) {
  const clip = useId();
  const club = findClub(slug);
  if (!club) {
    return (
      <span
        aria-hidden="true"
        className={`inline-block shrink-0 rounded-full bg-line-strong ${className}`}
        style={{ width: size * 0.6, height: size * 0.6 }}
      />
    );
  }
  const { badge } = club;
  const initial = club.name.charAt(0);

  return (
    <svg
      viewBox="0 0 32 38"
      width={size}
      height={Math.round(size * 1.19)}
      aria-hidden="true"
      focusable="false"
      className={`shrink-0 ${className}`}
    >
      <defs>
        <clipPath id={clip}>
          <path d={SHIELD} />
        </clipPath>
      </defs>
      <g clipPath={`url(#${clip})`}>
        <rect width="32" height="38" fill={badge.primary} />
        <Pattern badge={badge} />
      </g>
      <path d={SHIELD} fill="none" stroke="rgba(236,239,243,0.55)" strokeWidth="1.5" />
      <text
        x="16"
        y="24"
        textAnchor="middle"
        fontFamily="var(--font-display)"
        fontWeight="800"
        fontSize="15"
        fill={badge.ink}
        stroke={badge.ink === "#FFFFFF" ? "rgba(0,0,0,0.45)" : "rgba(255,255,255,0.6)"}
        strokeWidth="2.2"
        paintOrder="stroke"
      >
        {initial}
      </text>
    </svg>
  );
}
