async function main() {
  if (!process.argv.includes('--confirm-demo-reset')) throw new Error('Thêm --confirm-demo-reset để xóa dữ liệu của Emulator demo cục bộ.');
  const endpoints = [
    'http://127.0.0.1:8080/emulator/v1/projects/demo-student-manager/databases/(default)/documents',
    'http://127.0.0.1:9099/emulator/v1/projects/demo-student-manager/accounts'
  ];
  for (const url of endpoints) {
    const response = await fetch(url, { method: 'DELETE', signal: AbortSignal.timeout(30000) });
    if (!response.ok) throw new Error('Không xóa được dữ liệu Emulator: ' + response.status);
  }
  console.log('Đã xóa dữ liệu demo cục bộ. Chạy lại script seed để tạo dữ liệu mẫu.');
}
main().catch(error => { console.error(error.message); process.exitCode = 1; });
