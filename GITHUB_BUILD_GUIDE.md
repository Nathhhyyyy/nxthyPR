# How to Build your APK for FREE (No Software Needed)

Since your laptop has limited storage (5GB), do not install Android Studio. Instead, use this "Cloud Build" method.

### Step 1: Create a GitHub Account
If you don't have one, sign up at [github.com](https://github.com).

### Step 2: Create a New Repository
1. Click the **+** icon at the top right and select **New repository**.
2. Name it `nxthyPR`.
3. Set it to **Public** or **Private** (doesn't matter).
4. Click **Create repository**.

### Step 3: Upload the Files
1. Unzip the `nxthyPR_source.zip` file I gave you.
2. In your GitHub repository, click **uploading an existing file**.
3. Drag and drop all the files from the `nxthyPR` folder (including the `.github` folder) into the browser.
4. Click **Commit changes**.

### Step 4: Run the Build
1. Go to the **Actions** tab in your GitHub repository.
2. You will see a workflow named "Android CI".
3. If it doesn't start automatically, click on "Android CI" on the left, then click **Run workflow**.
4. Wait about 2-3 minutes for the green checkmark ✅.

### Step 5: Download your APK
1. Click on the completed build run.
2. Scroll down to the **Artifacts** section.
3. Click on `nxthyPR-debug-apk` to download it.
4. Unzip the downloaded file to find your `app-debug.apk`.

**That's it! You now have your APK without using any space on your laptop.**
