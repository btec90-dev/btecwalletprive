const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { serverTimestamp } = require("firebase/firestore");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_EMAIL = "410bala@gmail.com";
const SECONDARY_EMAIL = "traore200@gmail.com";
const OUTSIDER_EMAIL = "outsider@example.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function adminContext() {
  return testEnv.authenticatedContext("uid_admin", {
    email: ADMIN_EMAIL,
    email_verified: true,
  });
}

function secondaryContext() {
  return testEnv.authenticatedContext("uid_secondary", {
    email: SECONDARY_EMAIL,
    email_verified: true,
  });
}

function outsiderContext() {
  return testEnv.authenticatedContext("uid_outsider", {
    email: OUTSIDER_EMAIL,
    email_verified: true,
  });
}

function unverifiedAdminContext() {
  return testEnv.authenticatedContext("uid_unverified", {
    email: ADMIN_EMAIL,
    email_verified: false,
  });
}

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

test("Unauthenticated user: denied read/write on shared_wallets and private_twt", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("shared_wallets").doc("balatec_shared").collection("accounts").get()
  );
  await assertFails(
    unauthDb.collection("private_twt").doc("admin_only").collection("accounts").get()
  );
});

test("Unauthorized email: denied read/write on shared_wallets and private_twt", async () => {
  const db = outsiderContext().firestore();
  await assertFails(
    db.collection("shared_wallets").doc("balatec_shared").collection("accounts").doc("acc1").set({
      id: "acc1",
      name: "Tap Tap Send",
      type: "CUSTOM",
      balance: 10000,
      currencyCode: "CFA",
      isTwt: false,
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    db.collection("private_twt").doc("admin_only").collection("accounts").get()
  );
});

test("Unverified email claim: denied even if email matches admin", async () => {
  const db = unverifiedAdminContext().firestore();
  await assertFails(
    db.collection("private_twt").doc("admin_only").collection("accounts").get()
  );
  await assertFails(
    db.collection("shared_wallets").doc("balatec_shared").collection("accounts").get()
  );
});

test("Authorized shared users (410bala & traore200): share read/write on non-TWT accounts & transactions", async () => {
  const adminDb = adminContext().firestore();
  const secDb = secondaryContext().firestore();

  const sharedAccRef = adminDb
    .collection("shared_wallets")
    .doc("balatec_shared")
    .collection("accounts")
    .doc("acc_taptap");

  await assertSucceeds(
    sharedAccRef.set({
      id: "acc_taptap",
      name: "Tap Tap Send",
      type: "CUSTOM",
      balance: 125000,
      currencyCode: "CFA",
      emojiAvatar: "📲",
      isTwt: false,
      updatedAt: serverTimestamp(),
    })
  );

  const secAccRef = secDb
    .collection("shared_wallets")
    .doc("balatec_shared")
    .collection("accounts")
    .doc("acc_taptap");

  await assertSucceeds(secAccRef.get());

  const secTxRef = secDb
    .collection("shared_wallets")
    .doc("balatec_shared")
    .collection("transactions")
    .doc("tx_family_1");

  await assertSucceeds(
    secTxRef.set({
      id: "tx_family_1",
      accountId: "acc_taptap",
      type: "EXPENSE",
      amount: 50000,
      payee: "Drissa Traoré",
      category: "Famille",
      timestamp: Date.now(),
      currencyCode: "CFA",
      isTwt: false,
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    adminDb
      .collection("shared_wallets")
      .doc("balatec_shared")
      .collection("transactions")
      .doc("tx_family_1")
      .get()
  );
});

test("Strict TWT isolation: TWT accounts rejected in shared_wallets", async () => {
  const secDb = secondaryContext().firestore();
  await assertFails(
    secDb.collection("shared_wallets").doc("balatec_shared").collection("accounts").doc("twt_sneak").set({
      id: "twt_sneak",
      name: "TWT 2026",
      type: "SAVINGS",
      balance: 100,
      currencyCode: "USD",
      isTwt: true,
      updatedAt: serverTimestamp(),
    })
  );
});

test("Strict TWT isolation: traore200@gmail.com denied all access to private_twt while 410bala@gmail.com succeeds", async () => {
  const adminDb = adminContext().firestore();
  const secDb = secondaryContext().firestore();

  const twtAccRef = adminDb
    .collection("private_twt")
    .doc("admin_only")
    .collection("accounts")
    .doc("acc_twt_2026");

  await assertSucceeds(
    twtAccRef.set({
      id: "acc_twt_2026",
      name: "TWT 2026",
      type: "SAVINGS",
      balance: 500,
      currencyCode: "USD",
      emojiAvatar: "🪙",
      isTwt: true,
      updatedAt: serverTimestamp(),
    })
  );

  const twtTxRef = adminDb
    .collection("private_twt")
    .doc("admin_only")
    .collection("transactions")
    .doc("tx_twt_bonus");

  await assertSucceeds(
    twtTxRef.set({
      id: "tx_twt_bonus",
      accountId: "acc_twt_2026",
      type: "INCOME",
      amount: 250,
      payee: "Employer",
      category: "Bonus",
      timestamp: Date.now(),
      currencyCode: "USD",
      isTwt: true,
      updatedAt: serverTimestamp(),
    })
  );

  // Secondary user must be denied get, list, create, update, and delete on private_twt
  await assertFails(
    secDb.collection("private_twt").doc("admin_only").collection("accounts").doc("acc_twt_2026").get()
  );
  await assertFails(
    secDb.collection("private_twt").doc("admin_only").collection("accounts").get()
  );
  await assertFails(
    secDb.collection("private_twt").doc("admin_only").collection("transactions").doc("tx_twt_bonus").get()
  );
  await assertFails(
    secDb.collection("private_twt").doc("admin_only").collection("transactions").doc("tx_twt_hack").set({
      id: "tx_twt_hack",
      accountId: "acc_twt_2026",
      type: "EXPENSE",
      amount: 10,
      payee: "Test",
      category: "Bonus",
      timestamp: Date.now(),
      currencyCode: "USD",
      isTwt: true,
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    secDb.collection("private_twt").doc("admin_only").collection("accounts").doc("acc_twt_2026").delete()
  );
});

test("Validation: negative or zero transaction amount is rejected", async () => {
  const adminDb = adminContext().firestore();
  await assertFails(
    adminDb.collection("shared_wallets").doc("balatec_shared").collection("transactions").doc("tx_bad").set({
      id: "tx_bad",
      accountId: "acc_taptap",
      type: "EXPENSE",
      amount: -50,
      payee: "Drissa Traoré",
      category: "Famille",
      timestamp: Date.now(),
      currencyCode: "CFA",
      isTwt: false,
      updatedAt: serverTimestamp(),
    })
  );
});
