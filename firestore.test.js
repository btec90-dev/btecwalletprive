const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

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

test("Unauthenticated user: cannot read accounts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("accounts").get());
});

test("Authenticated user: cannot read another user's accounts", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("accounts").get());
});

test("Authenticated user: can create and read their own accounts", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const accDoc = aliceDb.collection("users").doc(ALICE_UID).collection("accounts").doc("acc1");
  await assertSucceeds(
    accDoc.set({
      name: "Taptap Send",
      type: "MOBILE_MONEY",
      balance: 100000,
      currencyCode: "CFA"
    })
  );
  await assertSucceeds(accDoc.get());
});

test("Authenticated user: can create and read their own transactions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const txDoc = aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx1");
  await assertSucceeds(
    txDoc.set({
      amount: 50000,
      currencyCode: "CFA",
      type: "EXPENSE",
      category: "Famille",
      accountId: "acc1",
      timestamp: Date.now()
    })
  );
  await assertSucceeds(txDoc.get());
});

test("Authenticated user: cannot write transaction with invalid schema (e.g. non-positive amount)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const txDoc = aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx2");
  await assertFails(
    txDoc.set({
      amount: -100,
      currencyCode: "CFA",
      type: "EXPENSE",
      category: "Famille",
      accountId: "acc1",
      timestamp: Date.now()
    })
  );
});
