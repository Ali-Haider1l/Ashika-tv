const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_user_123";
const BOB_UID = "bob_user_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any user collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 2000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      activeProfile: "Family",
      appLanguage: "bn",
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("favorites").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("channels").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("playlists").get());
  await assertFails(unauthDb.collection("other_root").get());
});

test("Owner can create, update, and read their user profile with strict schema and immutable fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      activeProfile: "Family",
      appLanguage: "bn",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update allowed
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      activeProfile: "Sports",
      appLanguage: "en",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject invalid language code
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      appLanguage: "xx",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      activeProfile: "Kids",
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Owner can manage favorites, custom channels, and playlists while @DocumentId id keys are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Empty collection list succeeds (no resource.data reference on list)
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("favorites").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("channels").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("playlists").get());

  // Favorite create + update + delete
  const favRef = aliceDb.collection("users").doc(ALICE_UID).collection("favorites").doc("fav_somoy_tv");
  await assertSucceeds(
    favRef.set({
      userId: ALICE_UID,
      profileName: "Family",
      channelUrl: "https://example.com/somoy.m3u8",
      channelName: "Somoy TV",
      groupTitle: "News",
      logoUrl: "https://example.com/somoy.png",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    favRef.update({
      channelName: "Somoy TV HD",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Custom channel create + update
  const chRef = aliceDb.collection("users").doc(ALICE_UID).collection("channels").doc("ch_custom_1");
  await assertSucceeds(
    chRef.set({
      userId: ALICE_UID,
      channelName: "Custom Sports",
      streamUrl: "https://example.com/sports.m3u8",
      groupTitle: "Sports",
      logoUrl: "",
      tvgId: "sports.1",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    chRef.update({
      channelName: "Custom Sports HD",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Playlist create + update
  const plRef = aliceDb.collection("users").doc(ALICE_UID).collection("playlists").doc("pl_hdtv_1");
  await assertSucceeds(
    plRef.set({
      userId: ALICE_UID,
      title: "FreeHunter HDTV Live",
      playlistUrl: "https://example.com/HDTV.m3u",
      channelCount: 42,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    plRef.update({
      channelCount: 45,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject @DocumentId 'id' inside document payload
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("channels").doc("ch_bad").set({
      id: "ch_bad",
      userId: ALICE_UID,
      channelName: "Bad Channel",
      streamUrl: "https://example.com/bad.m3u8",
      groupTitle: "Other",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(favRef.delete());
  await assertSucceeds(chRef.delete());
  await assertSucceeds(plRef.delete());
});

test("Cross-user isolation blocks Bob from reading or modifying Alice's documents", async () => {
  const past = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      activeProfile: "Family",
      appLanguage: "bn",
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("users").doc(ALICE_UID).collection("favorites").doc("fav_1").set({
      userId: ALICE_UID,
      profileName: "Family",
      channelUrl: "https://example.com/live.m3u8",
      channelName: "Channel 1",
      createdAt: past,
      updatedAt: past,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("favorites").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("favorites").doc("fav_1").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("favorites").doc("fav_1").delete()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("channels").doc("ch_hijack").set({
      userId: BOB_UID,
      channelName: "Hijacked",
      streamUrl: "https://example.com/h.m3u8",
      groupTitle: "News",
      createdAt: past,
      updatedAt: past,
    })
  );
});
