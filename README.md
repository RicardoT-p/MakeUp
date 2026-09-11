# 妆鉴 · 妆容参照

Vue 3 + MediaPipe Face Landmarker + Spring Boot + MyBatis + MySQL + Redis 的可运行首版。照片在浏览器本地分析，后端只接收比例、质量分和推荐结果。

## 本地运行

要求：Node.js 20+、Java 17+、Maven 3.9+、Docker。

```bash
docker compose up -d
cd backend && mvn spring-boot:run
cd frontend && npm install && npm run dev
```

打开 `http://localhost:5173`。Swagger UI：`http://localhost:8080/swagger-ui.html`，OpenAPI JSON：`http://localhost:8080/v3/api-docs`。

## HTTPS 与 API 约定

- 所有资源使用版本化 REST 路径 `/api/v1/...`，创建返回 `201`，删除返回 `204`，错误使用 RFC 9457 `application/problem+json`。
- 生产环境推荐由 Nginx / 网关终止 TLS，并设置 `X-Forwarded-Proto`；应用已启用转发头处理。
- 若应用直接终止 TLS，启用 `prod` profile，并设置 `TLS_KEY_STORE` 与 `TLS_KEY_STORE_PASSWORD`。只允许 HTTPS，生产网关应补充 HSTS。
- 原始照片不离开浏览器；接口只接受数值特征。博主写接口上线前应接入管理员鉴权。

## 视觉模型选择

采用 MediaPipe Face Landmarker：官方 Web 运行时可在浏览器中输出 478 个三维人脸关键点，适合眼、眉、鼻、唇与轮廓比例。OpenCV 保留给离线数据质检；YOLO 默认姿态模型是人体 17 点，要做细粒度人脸比例需另建关键点数据集训练，因此首版不采用。

比例是二维投影近似值，受镜头焦距、角度、表情、滤镜和遮挡影响。上庭没有可靠发际线关键点，目前以额头顶部关键点近似；若用于正式产品，应建立经过人工标注、不同机型和人群覆盖的验证集，并校准阈值。

## 数据与运营

每次合格分析都会按轮廓、唇妆和眼妆生成检索词，通过 360 搜索的公开结果页查找抖音、小红书内容，并将结果写入 MySQL 后返回。`data.sql` 不再预置虚构博主；数据库只保存公开标题、平台链接、标签和触发该次检索的比例画像。免费搜索可能出现验证码或页面结构调整，正式运营时应更换为有服务协议保障的搜索接口。
