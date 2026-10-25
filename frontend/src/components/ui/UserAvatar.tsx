import { initials } from "@/lib/format";

/** Monogram avatar: no uploads in the MVP, so initials on a chalk-ringed disc. */
export function UserAvatar({ name, size = 40 }: { name: string; size?: number }) {
  return (
    <span
      aria-hidden="true"
      className="inline-flex shrink-0 items-center justify-center rounded-full border border-chalk/40 bg-pitch-raised font-display font-bold text-chalk"
      style={{ width: size, height: size, fontSize: Math.round(size * 0.42) }}
    >
      {initials(name)}
    </span>
  );
}
