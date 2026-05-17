# Deploy Verifier Portal

Domain: `verify.michikuni.cloud` — static SPA served by Nginx, backend tại `michikuni.cloud`.

---

## 1. Build trên VPS

```bash
# Upgrade Node.js nếu version < 14
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs

# Build
cd ~/identity-fabric/verifier-portal
npm install
VITE_BACKEND_URL=https://michikuni.cloud npm run build
```

## 2. Copy dist vào thư mục Nginx

```bash
sudo mkdir -p /var/www/verifier-portal
sudo cp -r ~/identity-fabric/verifier-portal/dist/* /var/www/verifier-portal/
sudo chown -R www-data:www-data /var/www/verifier-portal
```

## 3. Cấu hình Nginx

Tạo `/etc/nginx/sites-available/verifier-portal`:

```nginx
server {
    listen 80;
    server_name verify.michikuni.cloud;

    root /var/www/verifier-portal;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location ~* \.(js|css|png|jpg|ico|svg|woff2?)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/verifier-portal /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

> Nếu Certbot thêm config vào `sites-enabled/default` thay vì file riêng, xóa symlink trùng và đảm bảo `root` trỏ đúng về `/var/www/verifier-portal`.

## 4. HTTPS (Certbot)

```bash
sudo certbot --nginx -d verify.michikuni.cloud
```

## 5. Cập nhật sau này

```bash
cd ~/identity-fabric
git pull
cd verifier-portal
npm install
VITE_BACKEND_URL=https://michikuni.cloud npm run build
sudo cp -r dist/* /var/www/verifier-portal/
sudo chown -R www-data:www-data /var/www/verifier-portal
```

## Lưu ý

- `VITE_BACKEND_URL` baked vào lúc build — đổi URL backend phải build lại.
- Nginx phải serve từ `/var/www/` chứ không phải `/home/` vì `www-data` không có quyền đọc home directory.
- DNS wildcard `*` đã có sẵn nên không cần thêm record cho subdomain mới.
