const path = require('path')
const XLSX = require('../tools/node_modules/xlsx')

const rows = [
  ['日期', '学习资料名称', '资料类型', '核心内容总结', '个人心得', '对应项目阶段'],
  ['2026-02-17', 'Spring Boot 官方文档', '技术资料', '学习控制器、参数校验、REST API 设计与项目启动方式。', '理解了后端分层与接口设计的基本规范。', '系统设计'],
  ['2026-02-19', 'Vue 3 + Vite 开发文档', '技术资料', '梳理组合式 API、组件通信、工程结构和打包流程。', '前端项目初始化和页面拆分思路更清晰。', '编码实现'],
  ['2026-02-22', 'MySQL 8 数据库设计资料', '文献', '学习主外键设计、索引设置和停车业务表结构拆分方法。', '数据库设计必须从业务流程反推，而不是只看字段。', '数据库设计'],
  ['2026-02-26', '软件工程课程资料', '课程资料', '复盘需求分析、UML 建模、测试用例设计和文档整理流程。', '毕设不只是代码，文档和过程材料同样重要。', '需求分析'],
  ['2026-03-02', '前后端联调记录', '学习记录', '整理了前端调用后端接口、异常处理与演示兜底方案。', '联调阶段要优先保证主流程稳定，再优化细节。', '联调测试'],
  ['2026-03-06', '毕业实习指南', '指导材料', '明确了报告册、学习日志、UML 图、测试与部署等必交内容。', '越早同步整理材料，后续越不容易返工。', '材料整理'],
]

const workbook = XLSX.utils.book_new()
const worksheet = XLSX.utils.aoa_to_sheet(rows)
worksheet['!cols'] = [
  { wch: 14 },
  { wch: 30 },
  { wch: 14 },
  { wch: 46 },
  { wch: 46 },
  { wch: 16 },
]
XLSX.utils.book_append_sheet(workbook, worksheet, '学习日志')

const outputPath = path.resolve(__dirname, '../deliverables/学习日志.xlsx')
XLSX.writeFile(workbook, outputPath)
console.log(`Generated ${outputPath}`)
