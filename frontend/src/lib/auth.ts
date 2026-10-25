import "server-only";

import { redirect } from "next/navigation";

import { loginHref } from "@/lib/navigation";
import { getMe } from "@/lib/server-api";
import type { Me } from "@/types/api";

/** For signed-in pages: returns the visitor or sends them to log in, then back to `returnTo`. */
export async function requireMe(returnTo: string): Promise<Me> {
  const me = await getMe();
  if (!me) redirect(loginHref(returnTo));
  return me;
}

export function isStaffRole(me: Me): boolean {
  return me.role === "MODERATOR" || me.role === "ADMIN";
}
