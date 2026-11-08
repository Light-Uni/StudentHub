from pathlib import Path
import shutil
import sys
import zipfile

sys.stdout.reconfigure(encoding='utf-8')
root = Path(__file__).resolve().parents[1]
apk = root / 'app/build/outputs/apk/emulator/debug/app-emulator-debug.apk'
if not apk.is_file():
    raise SystemExit('Chưa có APK. Hãy chạy gradlew.bat assembleEmulatorDebug trước.')
destination = root / 'submit'
destination.mkdir(exist_ok=True)
folders = ['app/src', 'gradle/wrapper', 'functions/scripts', 'functions/test', 'samples', 'docs', 'tools']
files = ['README.md', 'settings.gradle', 'build.gradle', 'gradle.properties', 'gradlew', 'gradlew.bat',
         'app/build.gradle', 'firebase.json', 'firestore.rules', 'firestore.indexes.json',
         'functions/package.json', 'functions/package-lock.json', 'functions/index.js', 'functions/validation.js', '.gitignore']
with zipfile.ZipFile(destination / 'SoSinhVien-source.zip', 'w', zipfile.ZIP_DEFLATED) as archive:
    for name in files:
        source = root / name
        if name == 'README.md' and not source.is_file():
            source = destination / name
        archive.write(source, name)
    for folder in folders:
        for path in (root / folder).rglob('*'):
            if path.is_file() and '__pycache__' not in path.parts:
                archive.write(path, path.relative_to(root))
for folder in ['docs', 'samples']:
    shutil.copytree(root / folder, destination / folder, dirs_exist_ok=True)
shutil.copy2(apk, destination / 'SoSinhVien-emulator-debug.apk')
print('Đã đóng gói mã nguồn và APK thử nghiệm vào submit/. Không kèm cache, cấu hình máy hoặc khóa Firebase.')
