<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from './services/api'
import type { AppDataset, ParkingLotPayload, User, VehiclePayload } from './types'

const views = ['dashboard', 'admin', 'user', 'docs'] as const

const dataset = ref<AppDataset | null>(null)
const loading = ref(true)
const banner = ref('正在加载城市停车资源管理系统演示数据...')
const selectedView = ref<'dashboard' | 'admin' | 'user' | 'docs'>('dashboard')
const currentAccount = ref('admin')
const loginState = reactive({ username: 'admin', password: '123456' })

const parkingLotForm = reactive<ParkingLotPayload>({
  name: '',
  code: '',
  address: '',
  totalSpaces: 20,
  businessHours: '06:00-23:30',
})

const vehicleForm = reactive<VehiclePayload>({
  userId: 3,
  plateNumber: '',
  brand: '',
  color: '',
})

const checkInForm = reactive({
  vehicleId: 1,
  parkingLotId: 1,
})

const activeRecords = computed(() => dataset.value?.records.filter((item) => item.status === '在场') ?? [])
const ownerUsers = computed(() => dataset.value?.users.filter((item) => item.role === '车主') ?? [])
const currentUser = computed<User | undefined>(() =>
  dataset.value?.users.find((item) => item.username === currentAccount.value),
)

async function refresh() {
  dataset.value = await api.getDataset()
}

async function loadPage() {
  loading.value = true
  await refresh()
  banner.value = '系统已就绪。若本地后端未启动，页面将自动使用内置演示数据。'
  loading.value = false
}

async function doLogin() {
  try {
    const result = await api.login(loginState)
    currentAccount.value = result.user.username
    banner.value = `当前演示身份已切换为：${result.user.realName}（${result.user.role}）`
  } catch (error) {
    banner.value = error instanceof Error ? error.message : '登录失败'
  }
}

async function addParkingLot() {
  try {
    await api.createParkingLot(parkingLotForm)
    await refresh()
    Object.assign(parkingLotForm, {
      name: '',
      code: '',
      address: '',
      totalSpaces: 20,
      businessHours: '06:00-23:30',
    })
    banner.value = '停车场资源新增成功，答辩时可用于演示资源扩容。'
  } catch (error) {
    banner.value = error instanceof Error ? error.message : '新增停车场失败'
  }
}

async function addVehicle() {
  try {
    await api.createVehicle(vehicleForm)
    await refresh()
    Object.assign(vehicleForm, {
      userId: 3,
      plateNumber: '',
      brand: '',
      color: '',
    })
    banner.value = '车辆档案已新增，用户端数据已同步刷新。'
  } catch (error) {
    banner.value = error instanceof Error ? error.message : '新增车辆失败'
  }
}

async function checkInVehicle() {
  try {
    await api.checkIn(checkInForm.vehicleId, checkInForm.parkingLotId)
    await refresh()
    banner.value = '车辆已成功入场，车位状态和停车记录已自动更新。'
  } catch (error) {
    banner.value = error instanceof Error ? error.message : '车辆入场失败'
  }
}

async function checkOutVehicle(recordId: number) {
  try {
    await api.checkOut(recordId)
    await refresh()
    banner.value = '车辆已出场并完成费用结算，订单与统计数据已更新。'
  } catch (error) {
    banner.value = error instanceof Error ? error.message : '车辆出场失败'
  }
}

const usageRate = computed(() => dataset.value?.overview.occupancyRate ?? '0%')

function setView(view: (typeof views)[number]) {
  selectedView.value = view
}

onMounted(loadPage)
</script>

<template>
  <div class="page-shell" v-if="dataset">
    <header class="hero">
      <div class="hero-copy">
        <span class="eyebrow">Graduation Project 2026</span>
        <h1>{{ dataset.projectInfo.projectName }}</h1>
        <p>
          围绕“城市停车资源管理”完成停车资源维护、车辆进出场、订单结算、可视化看板、调度建议与材料整理，
          适合作为毕设演示与文档交付的一体化作品。
        </p>
        <div class="hero-actions">
          <button
            v-for="view in views"
            :key="view"
            class="pill"
            :class="{ active: selectedView === view }"
            @click="setView(view)"
          >
            {{
              view === 'dashboard'
                ? '数据看板'
                : view === 'admin'
                  ? '管理端'
                  : view === 'user'
                    ? '用户端'
                    : '交付材料'
            }}
          </button>
        </div>
      </div>
      <aside class="hero-panel">
        <div class="status-card">
          <p class="status-label">当前演示账号</p>
          <strong>{{ currentUser?.realName }}</strong>
          <span>{{ currentUser?.role }}</span>
        </div>
        <div class="status-card">
          <p class="status-label">总体车位占用率</p>
          <strong>{{ usageRate }}</strong>
          <span>实时联动停车记录与车位资源</span>
        </div>
        <div class="status-card banner-card">
          <p class="status-label">系统提示</p>
          <span>{{ banner }}</span>
        </div>
      </aside>
    </header>

    <section class="section team-grid">
      <article v-for="member in dataset.projectInfo.team" :key="member.name" class="team-card">
        <span>{{ member.role }}</span>
        <h3>{{ member.name }}</h3>
        <p>{{ member.responsibility }}</p>
      </article>
    </section>

    <section v-if="selectedView === 'dashboard'" class="section">
      <div class="card-grid metrics">
        <article v-for="metric in dataset.overview.metrics" :key="metric.title" class="metric-card">
          <span>{{ metric.title }}</span>
          <strong>{{ metric.value }}</strong>
          <p>{{ metric.description }}</p>
        </article>
      </div>

      <div class="dual-grid">
        <article class="panel">
          <div class="panel-header">
            <h2>停车流量趋势</h2>
            <span>分时段入场 / 出场统计</span>
          </div>
          <div class="trend-list">
            <div v-for="point in dataset.overview.parkingTrend" :key="point.time" class="trend-row">
              <span class="time">{{ point.time }}</span>
              <div class="bars">
                <div class="bar bar-entry" :style="{ width: `${point.entries * 3}px` }">
                  入场 {{ point.entries }}
                </div>
                <div class="bar bar-exit" :style="{ width: `${point.exits * 3}px` }">
                  出场 {{ point.exits }}
                </div>
              </div>
            </div>
          </div>
        </article>

        <article class="panel">
          <div class="panel-header">
            <h2>资源调度建议</h2>
            <span>基于占用率生成的运营建议</span>
          </div>
          <div class="suggestion-list">
            <div v-for="item in dataset.dispatchSuggestions" :key="item.parkingLotName" class="suggestion-card">
              <div>
                <h3>{{ item.parkingLotName }}</h3>
                <span>{{ item.warningLevel }}风险 / {{ item.occupancyRate }}</span>
              </div>
              <p>{{ item.action }}</p>
            </div>
          </div>
        </article>
      </div>
    </section>

    <section v-if="selectedView === 'admin'" class="section">
      <div class="dual-grid">
        <article class="panel">
          <div class="panel-header">
            <h2>停车场资源管理</h2>
            <span>管理员 / 停车场管理员核心工作台</span>
          </div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>名称</th>
                  <th>编码</th>
                  <th>车位</th>
                  <th>空闲</th>
                  <th>营业时间</th>
                  <th>状态</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="lot in dataset.parkingLots" :key="lot.id">
                  <td>{{ lot.name }}</td>
                  <td>{{ lot.code }}</td>
                  <td>{{ lot.totalSpaces }}</td>
                  <td>{{ lot.freeSpaces }}</td>
                  <td>{{ lot.businessHours }}</td>
                  <td><span class="badge success">{{ lot.status }}</span></td>
                </tr>
              </tbody>
            </table>
          </div>
          <form class="compact-form" @submit.prevent="addParkingLot">
            <input v-model="parkingLotForm.name" placeholder="新增停车场名称" />
            <input v-model="parkingLotForm.code" placeholder="停车场编码" />
            <input v-model="parkingLotForm.address" placeholder="停车场地址" />
            <input v-model.number="parkingLotForm.totalSpaces" type="number" min="1" placeholder="总车位数" />
            <input v-model="parkingLotForm.businessHours" placeholder="营业时间" />
            <button type="submit">新增停车场</button>
          </form>
        </article>

        <article class="panel">
          <div class="panel-header">
            <h2>车位与收费规则</h2>
            <span>支撑资源管理与费用计算</span>
          </div>
          <div class="space-tags">
            <span v-for="space in dataset.spaces" :key="space.id" class="tag" :class="space.status === '占用' ? 'danger' : 'normal'">
              {{ space.code }} / {{ space.category }} / {{ space.status }}
            </span>
          </div>
          <div class="pricing-list">
            <div v-for="rule in dataset.pricingRules" :key="rule.id" class="pricing-item">
              <strong>{{ rule.parkingLotName }}</strong>
              <span>首 {{ rule.baseMinutes }} 分钟 {{ rule.baseFee }} 元，之后 {{ rule.hourlyFee }} 元/小时，封顶 {{ rule.dailyCap }} 元</span>
            </div>
          </div>
        </article>
      </div>

      <div class="dual-grid">
        <article class="panel">
          <div class="panel-header">
            <h2>停车记录与订单</h2>
            <span>支持现场管理与答辩演示</span>
          </div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>车牌</th>
                  <th>停车场</th>
                  <th>入场时间</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="record in dataset.records" :key="record.id">
                  <td>{{ record.plateNumber }}</td>
                  <td>{{ record.parkingLotName }}</td>
                  <td>{{ record.entryTime }}</td>
                  <td>
                    <span class="badge" :class="record.status === '在场' ? 'warning' : 'success'">{{ record.status }}</span>
                  </td>
                  <td>
                    <button v-if="record.status === '在场'" class="ghost-button" @click="checkOutVehicle(record.id)">
                      结算出场
                    </button>
                    <span v-else>{{ record.amount }} 元</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>

        <article class="panel">
          <div class="panel-header">
            <h2>系统公告与操作日志</h2>
            <span>支撑系统说明与测试记录</span>
          </div>
          <div class="notice-list">
            <div v-for="notice in dataset.notices" :key="notice.id" class="notice-card">
              <div class="notice-title">
                <strong>{{ notice.title }}</strong>
                <span class="badge">{{ notice.level }}</span>
              </div>
              <p>{{ notice.content }}</p>
            </div>
          </div>
          <div class="log-list">
            <div v-for="log in dataset.logs.slice(0, 6)" :key="log.id" class="log-item">
              <strong>{{ log.action }}</strong>
              <span>{{ log.detail }}</span>
              <small>{{ log.createdAt }}</small>
            </div>
          </div>
        </article>
      </div>
    </section>

    <section v-if="selectedView === 'user'" class="section">
      <div class="dual-grid">
        <article class="panel">
          <div class="panel-header">
            <h2>演示账号切换</h2>
            <span>用于展示管理员、停车场管理员、车主三种角色</span>
          </div>
          <form class="compact-form account-form" @submit.prevent="doLogin">
            <input v-model="loginState.username" placeholder="用户名：admin / manager / owner" />
            <input v-model="loginState.password" type="password" placeholder="密码：123456" />
            <button type="submit">切换身份</button>
          </form>
          <div class="account-cards">
            <div v-for="user in dataset.users" :key="user.id" class="account-card">
              <strong>{{ user.realName }}</strong>
              <span>{{ user.role }}</span>
              <p>{{ user.responsibility }}</p>
            </div>
          </div>
        </article>

        <article class="panel">
          <div class="panel-header">
            <h2>车辆档案与停车业务</h2>
            <span>车主端核心操作区</span>
          </div>
          <form class="compact-form" @submit.prevent="addVehicle">
            <select v-model.number="vehicleForm.userId">
              <option v-for="user in ownerUsers" :key="user.id" :value="user.id">
                {{ user.realName }}
              </option>
            </select>
            <input v-model="vehicleForm.plateNumber" placeholder="车牌号" />
            <input v-model="vehicleForm.brand" placeholder="品牌" />
            <input v-model="vehicleForm.color" placeholder="颜色" />
            <button type="submit">新增车辆</button>
          </form>

          <form class="compact-form" @submit.prevent="checkInVehicle">
            <select v-model.number="checkInForm.vehicleId">
              <option v-for="vehicle in dataset.vehicles" :key="vehicle.id" :value="vehicle.id">
                {{ vehicle.plateNumber }} / {{ vehicle.ownerName }}
              </option>
            </select>
            <select v-model.number="checkInForm.parkingLotId">
              <option v-for="lot in dataset.parkingLots" :key="lot.id" :value="lot.id">
                {{ lot.name }} / 空闲 {{ lot.freeSpaces }}
              </option>
            </select>
            <button type="submit">车辆入场</button>
          </form>

          <div class="vehicle-grid">
            <div v-for="vehicle in dataset.vehicles" :key="vehicle.id" class="vehicle-card">
              <strong>{{ vehicle.plateNumber }}</strong>
              <span>{{ vehicle.brand }} / {{ vehicle.color }}</span>
              <p>{{ vehicle.ownerName }}</p>
            </div>
          </div>
        </article>
      </div>

      <article class="panel">
        <div class="panel-header">
          <h2>当前在场车辆</h2>
          <span>用于演示进场后记录生成、出场后结算完成</span>
        </div>
        <div class="active-records">
          <div v-for="record in activeRecords" :key="record.id" class="active-card">
            <div>
              <strong>{{ record.plateNumber }}</strong>
              <span>{{ record.parkingLotName }} / {{ record.spaceCode }}</span>
            </div>
            <button class="ghost-button" @click="checkOutVehicle(record.id)">立即出场结算</button>
          </div>
        </div>
      </article>
    </section>

    <section v-if="selectedView === 'docs'" class="section">
      <div class="card-grid docs-grid">
        <article class="doc-card">
          <span>团队提交物</span>
          <h3>代码、技术文档、计划书、演示素材</h3>
          <p>仓库内已提供后端、前端、SQL、README、技术文档、计划书和个人材料模板。</p>
        </article>
        <article class="doc-card">
          <span>个人材料</span>
          <h3>毕业实习报告册 + 学习日志 Excel</h3>
          <p>报告册采用毕设常见结构，学习日志已生成可继续补充的 Excel 工作簿。</p>
        </article>
        <article class="doc-card">
          <span>UML / 数据库</span>
          <h3>Mermaid 图与表结构说明</h3>
          <p>技术文档内补齐用例图、类图、时序图、业务流程图、E-R 图和数据库表设计。</p>
        </article>
      </div>
    </section>
  </div>

  <div v-else class="loading-state">
    <strong>{{ loading ? '系统加载中...' : '暂无数据' }}</strong>
    <span>{{ banner }}</span>
  </div>
</template>
