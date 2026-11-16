# Hozin Web2APK — Builder HTML Drag & Drop

Aplikasi Android authoring HTML murni (native Kotlin). Bikin halaman web
dengan drag-and-drop ala Sketchware, lalu export sebagai ZIP berisi
`index.html` + folder `assets/`.

## Fitur v1.0
- Builder drag-drop: palet komponen (Judul, Teks, Gambar, Tombol, Kartu,
  Pembatas, Spasi, Video/YouTube, Navbar, Footer) — drag sungguhan ke canvas,
  urut ulang dengan drag, tap untuk memilih.
- Panel properti: teks, ukuran, warna, link, gambar dari galeri, tinggi spasi.
- Pratinjau native (render blok apa adanya, tanpa WebView).
- Undo / redo.
- Proyek lokal tersimpan sebagai JSON (buat / buka / duplikat / hapus).
- Generate `index.html` bersih + responsif, gambar sebagai file di `assets/`.
- Export ZIP via share.

## Batasan (jujur)
Aplikasi ini HANYA builder + export HTML/ZIP. **Tidak bisa convert halaman
menjadi APK** — bagian konverter website/HTML ke APK tidak dibangun di sini.

## Build
APK release dibangun otomatis oleh GitHub Actions saat push ke `main`
(keystore debug persisten dari secret `DEBUG_KEYSTORE_BASE64`), lalu
dirilis sebagai GitHub Release `v1.0`.
