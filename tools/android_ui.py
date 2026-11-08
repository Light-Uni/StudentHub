import argparse
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.stdout.reconfigure(encoding='utf-8')
PACKAGE = 'vn.edu.studentmanager.emulator'


def adb(*args):
    return subprocess.check_output(['adb', *args], timeout=50)


def tree():
    result = adb('shell', 'uiautomator', 'dump', '/sdcard/student-ui.xml').decode('utf-8', errors='replace')
    if 'dumped to' not in result:
        raise RuntimeError('Máy ảo chưa trả về cây giao diện hợp lệ: ' + result)
    raw = adb('shell', 'cat', '/sdcard/student-ui.xml')
    return ET.fromstring(raw)


def main():
    parser = argparse.ArgumentParser(description='Kiểm tra giao diện trên Android Emulator.')
    parser.add_argument('--tap')
    parser.add_argument('--text')
    parser.add_argument('--capture')
    args = parser.parse_args()
    if args.tap:
        nodes = tree().iter('node')
        match = next((n for n in nodes if n.get('resource-id', '').endswith('/' + args.tap) or n.get('text') == args.tap), None)
        if match is None:
            raise RuntimeError('Không tìm thấy phần tử: ' + args.tap)
        x1, y1, x2, y2 = map(int, re.findall(r'\d+', match.get('bounds')))
        adb('shell', 'input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2))
    if args.text:
        adb('shell', 'input', 'text', args.text.replace(' ', '%s'))
    if args.capture:
        target = ROOT / 'docs' / 'screenshots'
        target.mkdir(parents=True, exist_ok=True)
        adb('shell', 'screencap', '-p', '/sdcard/student-capture.png')
        adb('pull', '/sdcard/student-capture.png', str(target / (args.capture + '.png')))
        print('Đã chụp:', args.capture)
    if not args.text:
        for node in tree().iter('node'):
            if node.get('text') or node.get('content-desc'):
                print(node.get('resource-id'), node.get('text') or node.get('content-desc'), node.get('bounds'))


if __name__ == '__main__':
    main()
