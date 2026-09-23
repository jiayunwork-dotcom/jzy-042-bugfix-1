# camera-geometry-service

针孔投影 + Brown–Conrady 畸变 + 双视图三角化的后端服务。以「作业」为单位经 HTTP 交互：一次作业携带一批点，服务整批处理后返回结果。无前端页面。Java 17 + Spring Boot 3。

## 投影模型（钉死）

1. 相机坐标系三维点归一化到归一化平面：`x = X/Z, y = Y/Z`（**绝不**用像素坐标凑 r）。
2. 在归一化平面上施加 Brown–Conrady 畸变：
   - `r² = x² + y²`，径向缩放 `1 + k1·r² + k2·r⁴`
   - `x_d = x·radial + 2·p1·x·y + p2·(r² + 2x²)`
   - `y_d = y·radial + p1·(r² + 2y²) + 2·p2·x·y`
3. 内参映射到像素：`u = cx + fx·x_d`，`v = cy + fy·y_d`。
4. 判定像素是否落在 `[0,width) × [0,height)` 内，逐点如实标记。

## 三角化方法（固定选择）

**中点法（midpoint）**：每个匹配像素反投影成世界坐标系下的射线（相机中心 + 单位方向），取两条射线最近点对的中点作为三维点。绝不平均两个视图的像素坐标冒充三维点。反投影是投影的严格逆过程：像素先回到**带畸变的**归一化平面 `(u-cx)/fx, (v-cy)/fy`，再按该相机自己的 Brown–Conrady 系数做反畸变（Newton 迭代求畸变映射的逆），得到针孔射线上的归一化点后才反投成射线。每个匹配给出两个视图各自的重投影误差（重投影走与投影接口完全相同的函数，**带上该相机的畸变系数**），作业级报告最大与平均重投影误差（按每匹配两视图误差的均值聚合）。因此「带畸变相机投影 → 匹配点三角化 → 重投影核对」整条往返与零畸变同量级闭合。

## 作业策略（全局一致）

- **fail-fast**：任何非法输入在开算前整作业拒绝，返回带类型的结构化错误（HTTP 400）；不存在部分成功的作业，也不会对非法输入返回空结果。
- 畸变系数整体可省略（按零畸变处理）；一旦给出则四项必须齐全。投影作业在顶层给出一份；三角化作业在每台相机对象内各自给出（两台相机可以不同）。
- 服务无共享可变状态：并发作业完全隔离，各自的结果互不可见。

## 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/v1/projections/jobs` | 投影作业：内参+畸变+图像尺寸+一批三维点 → 每点像素坐标、是否在像面内、像面外点数 |
| POST | `/api/v1/projections/point` | 单点投影（与批量作业共用同一投影函数，结果一致） |
| POST | `/api/v1/triangulations/jobs` | 三角化作业：两台相机（内参+畸变+外参，畸变可省略按零处理）+ 匹配像点对 → 每对三维点与重投影误差、作业级最大/平均误差 |
| GET | `/api/v1/presets` | 只读回显已注册内参预设 |
| GET | `/api/v1/presets/cube-example` | 预置立方体标定算例（hd-1000 预设 + 单位立方体 8 角点） |
| POST | `/api/v1/presets/cube-example/run` | 跑一遍立方体算例投影，`outOfBoundsCount` 应为 0 |
| GET | `/api/v1/status` | 运行状态与作业计数 |

### 投影作业示例

```bash
curl -s localhost:8080/api/v1/projections/jobs -H 'Content-Type: application/json' -d '{
  "intrinsics": {"fx": 1000, "fy": 1000, "cx": 960, "cy": 540},
  "distortion": {"k1": -0.12, "k2": 0.015, "p1": 0.001, "p2": -0.0005},
  "image": {"width": 1920, "height": 1080},
  "points": [{"x": 0.3, "y": -0.2, "z": 2.0}, {"x": 5.0, "y": 0.0, "z": 1.0}]
}'
```

### 三角化作业示例

```bash
curl -s localhost:8080/api/v1/triangulations/jobs -H 'Content-Type: application/json' -d '{
  "camera1": {"intrinsics": {"fx": 1000, "fy": 1000, "cx": 960, "cy": 540},
              "distortion": {"k1": -0.28, "k2": 0.07, "p1": 0.0015, "p2": -0.0010},
              "pose": {"rotation": [[1,0,0],[0,1,0],[0,0,1]], "translation": [0,0,0]}},
  "camera2": {"intrinsics": {"fx": 1000, "fy": 1000, "cx": 960, "cy": 540},
              "distortion": {"k1": -0.18, "k2": 0.04, "p1": -0.0008, "p2": 0.0012},
              "pose": {"rotation": [[1,0,0],[0,1,0],[0,0,1]], "translation": [-1,0,0]}},
  "matches": [{"u1": 860, "v1": 490, "u2": 660, "v2": 490}]
}'
```

外参约定：`X_cam = R · X_world + t`，旋转为行主序 3×3。

### 错误响应

```json
{"type": "POINT_BEHIND_CAMERA", "message": "...", "details": {"path": "points", "pointIndices": [1, 2]}}
```

错误类型：`MISSING_INTRINSICS_FIELD`、`NON_POSITIVE_FOCAL_LENGTH`、`INVALID_IMAGE_SIZE`、`MISSING_DISTORTION_FIELD`、`EMPTY_POINT_LIST`、`INVALID_POINT_COORDINATE`、`POINT_BEHIND_CAMERA`、`MISSING_CAMERA`、`MISSING_CAMERA_POSE`、`INVALID_POSE`、`EMPTY_MATCH_LIST`、`INVALID_MATCH`、`TRIANGULATION_DEGENERATE`、`MALFORMED_REQUEST`、`INTERNAL_ERROR`。

## 构建与运行

```bash
docker build -t camera-geometry-service .
docker run --rm -p 8080:8080 camera-geometry-service
```

镜像构建阶段会跑完整测试套件，任何一条不变量失败都会让构建失败。本地开发：`mvn test` / `mvn spring-boot:run`。

## 自动化测试钉死的验收项

- 零畸变系数下畸变前后像素逐点相同（`PinholeProjectorInvariantsTest`）
- 点沿 Z 向远处平移，像点向主点收缩（零畸变下半径严格按 `z/(z+Δ)` 缩放）
- `fx`、`fy` 同时加倍，像点到主点半径随之加倍（零畸变与带畸变均成立）
- 归一化方向为 `X/Z`（防 `Z/X` 写反）
- 立方体角点三角化后重投影贴回原像素，误差 < 1e-6 px（`TriangulationJobServiceTest`）
- 两台带非零 k1/k2/p1/p2（且互不相同）的相机，投影→三角化→重投影整条往返同样 < 1e-6 px，三维角点恢复到 1e-6；相机省略 distortion 时按零畸变处理，行为不变（`TriangulationJobServiceTest` / `TriangulationDistortionWebTest`）
- 反畸变是畸变的严格逆：`undistort(distort(x,y)) ≈ (x,y)`，零畸变下为恒等（`BrownConradyDistortionTest`）
- `Z≤0`、焦距非正、图像尺寸为零、内参缺项、外参缺失、空匹配表分别被带类型拒绝（`ValidationWebTest`）
- 单点投影与批量作业同点结果逐位一致（`ConsistencyWebTest`）
- 16 路并发投影作业、8 路并发三角化作业结果互不串扰（`ConcurrencyIsolationWebTest`）
- 预置立方体算例 8 角点全部落在像面内（`PresetAndStatusWebTest`）

## 代码结构

```
com.camerageom
├── geometry    BrownConradyDistortion / PinholeProjector / MidpointTriangulator
├── model       Intrinsics / Distortion / CameraPose / CameraView / ...
├── validation  ErrorCode / JobValidationException / JobRequestValidator（fail-fast）
├── jobs        ProjectionJobService / TriangulationJobService / JobMetrics（无状态编排）
├── api         控制器 + 全局异常映射 + DTO
└── presets     内参预设注册表 + 立方体标定算例
```
