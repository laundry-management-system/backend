package com.laundry.common.security;

/** Resolves the authenticated JWT subject to the users.id foreign key used by business records. */
public interface ActorIdResolver {

    long resolveId(String username);
}
