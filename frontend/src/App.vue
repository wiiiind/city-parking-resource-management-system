<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from './services/api'
import type { AppDataset, LoginPayload, ParkingLotPayload, User, VehiclePayload } from './types'

const dataset = ref<AppDataset | null>(null)
const loading = ref(true)
const banner = ref('正在加载城市停车资源管理系统演示数据...')
const currentAccount = ref('')
const ownerTab = ref<'query' | 'vehicles' | 'records' | 'orders'>('query')
const adminTab = ref<'income' | 'records' | 'dashboard'>('income')
const lotCategoryFilter = ref<'全部' | '新能源' | '普通'>('全部')
const selectedLotId = ref(1)

const loginForm = reactive<LoginPayload>({
  username: 'owner',
  password: '123456',
})

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

const ownerNav = [
  { key: 'query', label: '停车查询' },
  { key: 'vehicles', label: '车辆管理' },
  { key: 'records', label: '停车记录' },
  { key: 'orders', label: '订单查看' },
] as const

const adminNav = [
  { key: 'income', label: '收入统计' },
  { key: 'records', label: '停车记录订单记录' },
  { key: 'dashboard', label: '单停车场看板' },
] as const

const currentUser = computed<User | undefined>(() =>
  dataset.value?.users.find((item) => item.username === currentAccount.value),
)
const loggedIn = computed(() => Boolean(currentUser.value))
const isOwnerMobileView = computed(() => currentUser.value?.role === '车主')
const ownerUsers = computed(() => dataset.value?.users.filter((item) => item.role === '车主') ?? [])
const ownerVehicles = computed(() =>
  dataset.value?.vehicles.filter((item) => item.userId === currentUser.value?.id) ?? [],
)
const ownerRecords = computed(() =>
  dataset.value?.records.filter((item) => item.ownerName === currentUser.value?.realName) ?? [],
)
const ownerOrders = computed(() =>
  dataset.value?.orders.filter((item) => ownerVehicles.value.some((vehicle) => vehicle.plateNumber === item.plateNumber)) ?? [],
)
const selectedLot = computed(() => dataset.value?.parkingLots.find((item) => item.id === selectedLotId.value))
const lotPricingMap = computed(() => {
  const entries: Array<[number, string]> =
    dataset.value?.pricingRules.map((rule) => [
      rule.parkingLotId,
      `首${rule.baseMinutes}分钟${rule.baseFee}元，之后${rule.hourlyFee}元/小时，封顶${rule.dailyCap}元`,
    ]) ?? []
  return new Map(entries)
})
const adminRevenueSummary = computed(() => {
  if (!dataset.value) {
    return []
  }
  return dataset.value.parkingLots.map((lot) => {
    const lotOrders = dataset.value?.orders.filter((order) => order.parkingLotName === lot.name) ?? []
    const income = lotOrders.reduce((sum, order) => sum + order.amount, 0)
    return {
      name: lot.name,
      orderCount: lotOrders.length,
      income,
      occupancyRate: lot.totalSpaces === 0 ? '0%' : `${((lot.occupiedSpaces / lot.totalSpaces) * 100).toFixed(1)}%`,
    }
  })
})
const lotDashboardSpaces = computed(() => {
  if (!dataset.value || !selectedLot.value) {
    return []
  }
  return dataset.value.spaces.filter((space) => {
    const sameLot = space.parkingLotId === selectedLot.value?.id
    const matchCategory = lotCategoryFilter.value === '全部' || space.category === lotCategoryFilter.value
    return sameLot && matchCategory
  })
})
const lotDashboardRecords = computed(() => {
  if (!dataset.value || !selectedLot.value) {
    return []
  }
  const vehicles = new Set(
    lotDashboardSpaces.value
      .filter((space) => space.status === '占用')
      .map((space) => space.code),
  )
  return dataset.value.records.filter((record) => {
    const sameLot = record.parkingLotId === selectedLot.value?.id
    const matchCategory =
      lotCategoryFilter.value === '全部' ||
      lotDashboardSpaces.value.some((space) => space.code === record.spaceCode) ||
      vehicles.has(record.spaceCode)
    return sameLot && matchCategory
  })
})
const lotDashboardMetrics = computed(() => {
  if (!selectedLot.value) {
    return []
  }
  const total = lotDashboardSpaces.value.length
  const occupied = lotDashboardSpaces.value.filter((item) => item.status === '占用').length
  const free = Math.max(0, total - occupied)
  const income = (dataset.value?.orders ?? [])
    .filter((order) => order.parkingLotName === selectedLot.value?.name)
    .reduce((sum, order) => sum + order.amount, 0)

  return [
    { title: '筛选后车位数', value: total, description: `${lotCategoryFilter.value}车位范围` },
    { title: '占用数量', value: occupied, description: '当前在场占用情况' },
    { title: '空闲数量', value: free, description: '可调度车位数量' },
    { title: '累计收入', value: `${income.toFixed(2)} 元`, description: '当前停车场订单累计' },
  ]
})
const usageRate = computed(() => dataset.value?.overview.occupancyRate ?? '0%')

async function refresh() {
  dataset.value = await api.getDataset()
  const lots = dataset.value?.parkingLots ?? []
  const firstLot = lots[0]
  if (firstLot && !lots.some((item) => item.id === selectedLotId.value)) {
    selectedLotId.value = firstLot.id
  }
}

async function loadPage() {
  loading.value = true
  await refresh()
  banner.value = '系统已就绪。默认提供车主演示登录，也可通过管理员入口进入后台。'
  loading.value = false
}

async function submitOwnerLogin() {
  try {
    await api.login(loginForm)
    const user = dataset.value?.users.find((item) => item.username === loginForm.username && item.role === '车主')
    if (!user) {
      throw new Error('请使用车主账号登录')
    }
    currentAccount.value = user.username
    ownerTab.value = 'query'
    banner.value = `已登录车主端：${user.realName}`
  } catch (error) {
    banner.value = error instanceof Error ? error.message : '车主登录失败'
  }
}

function enterAdminPortal() {
  const user = dataset.value?.users.find((item) => item.username === 'admin')
  if (!user) {
    banner.value = '未找到管理员账号'
    return
  }
  currentAccount.value = user.username
  adminTab.value = 'income'
  banner.value = `已进入管理员后台：${user.realName}`
}

function logout() {
  currentAccount.value = ''
  loginForm.username = 'owner'
  loginForm.password = '123456'
  banner.value = '已退出登录，请重新选择车主登录或管理员入口。'
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
    banner.value = '停车场资源新增成功。'
  } catch (error) {
    banner.value = error instanceof Error ? error.message : '新增停车场失败'
  }
}

async function addVehicle() {
  try {
    await api.createVehicle(vehicleForm)
    await refresh()
    Object.assign(vehicleForm, {
      userId: currentUser.value?.id ?? 3,
      plateNumber: '',
      brand: '',
      color: '',
    })
    banner.value = '车辆档案新增成功。'
  } catch (error) {
    banner.value = error instanceof Error ? error.message : '新增车辆失败'
  }
}

async function checkInVehicle() {
  try {
    await api.checkIn(checkInForm.vehicleId, checkInForm.parkingLotId)
    await refresh()
    ownerTab.value = 'records'
    banner.value = '已生成停车记录，可在停车记录页查看。'
  } catch (error) {
    banner.value = error instanceof Error ? error.message : '车辆入场失败'
  }
}

async function checkOutVehicle(recordId: number) {
  try {
    await api.checkOut(recordId)
    await refresh()
    ownerTab.value = isOwnerMobileView.value ? 'orders' : ownerTab.value
    banner.value = '停车费用已结算，订单信息已更新。'
  } catch (error) {
    banner.value = error instanceof Error ? error.message : '车辆出场失败'
  }
}

onMounted(loadPage)
</script>

<template>
  <div v-if="dataset && !loggedIn" class="login-shell">
    <section class="login-panel login-panel-split">
      <div class="login-copy">
        <span class="eyebrow">City Parking Resource Management</span>
        <h1>城市停车资源管理系统</h1>
        <p>系统以车主使用为主，支持停车查询、车辆绑定、停车记录查看和订单查询；管理员可通过单独入口进入后台查看经营数据。</p>
        <div class="login-tips">
          <span>车主主入口：账号密码登录</span>
          <span>管理员入口：单独按钮进入后台</span>
          <span>默认演示账号：`owner / 123456`</span>
        </div>
      </div>

      <div class="login-stack">
        <article class="login-card login-form-card">
          <span>车主登录</span>
          <strong>进入停车服务</strong>
          <p>登录后默认进入车主端导航，可切换查看停车查询、车辆管理、停车记录和订单查看。</p>
          <form class="stack-form" @submit.prevent="submitOwnerLogin">
            <input v-model="loginForm.username" placeholder="车主账号" />
            <input v-model="loginForm.password" type="password" placeholder="登录密码" />
            <button type="submit">登录车主端</button>
          </form>
        </article>

        <button class="login-card admin-entry-card" @click="enterAdminPortal">
          <span>系统管理员</span>
          <strong>进入后台管理</strong>
          <p>单独进入管理员板块，查看收入统计、停车记录订单记录和单停车场数据看板。</p>
          <small>点击进入管理后台</small>
        </button>
      </div>
    </section>
  </div>

  <div v-else-if="dataset" class="page-shell" :class="{ 'owner-mobile-shell': isOwnerMobileView }">
    <header class="hero">
      <div class="hero-copy">
        <span class="eyebrow">{{ isOwnerMobileView ? 'Owner Service Portal' : 'Admin Control Center' }}</span>
        <h1>{{ isOwnerMobileView ? '车主服务端' : '系统管理员后台' }}</h1>
        <p>
          {{
            isOwnerMobileView
              ? '通过统一导航完成停车查询、车辆管理、停车记录查看与订单查询，整体页面更贴近真实车主端使用逻辑。'
              : '后台聚焦经营分析与记录管理，通过三个导航页完成收入统计、停车记录订单记录和单停车场运营看板查看。'
          }}
        </p>
        <div class="hero-actions">
          <template v-if="isOwnerMobileView">
            <button
              v-for="item in ownerNav"
              :key="item.key"
              class="pill"
              :class="{ active: ownerTab === item.key }"
              @click="ownerTab = item.key"
            >
              {{ item.label }}
            </button>
          </template>
          <template v-else>
            <button
              v-for="item in adminNav"
              :key="item.key"
              class="pill"
              :class="{ active: adminTab === item.key }"
              @click="adminTab = item.key"
            >
              {{ item.label }}
            </button>
          </template>
          <button class="pill logout-pill" @click="logout">退出登录</button>
        </div>
      </div>
      <aside class="hero-panel">
        <div class="status-card">
          <p class="status-label">当前账号</p>
          <strong>{{ currentUser?.realName }}</strong>
          <span>{{ currentUser?.role }}</span>
        </div>
        <div class="status-card">
          <p class="status-label">总体车位占用率</p>
          <strong>{{ usageRate }}</strong>
          <span>实时联动停车记录与经营统计</span>
        </div>
        <div class="status-card banner-card">
          <p class="status-label">系统提示</p>
          <span>{{ banner }}</span>
        </div>
      </aside>
    </header>

    <section v-if="isOwnerMobileView" class="section">
      <article v-if="ownerTab === 'query'" class="panel owner-mobile-panel">
        <div class="panel-header">
          <h2>停车查询</h2>
          <span>查看停车场基础信息与收费规则</span>
        </div>
        <div class="notice-list">
          <div v-for="lot in dataset.parkingLots" :key="lot.id" class="notice-card">
            <div class="notice-title">
              <strong>{{ lot.name }}</strong>
              <span class="badge success">空闲 {{ lot.freeSpaces }} / {{ lot.totalSpaces }}</span>
            </div>
            <p>{{ lot.address }}</p>
            <p>营业时间：{{ lot.businessHours }}</p>
            <p>{{ lotPricingMap.get(lot.id) ?? '按停车场规则计费' }}</p>
          </div>
        </div>
        <form class="compact-form owner-checkin-form" @submit.prevent="checkInVehicle">
          <select v-model.number="checkInForm.vehicleId">
            <option v-for="vehicle in ownerVehicles" :key="vehicle.id" :value="vehicle.id">
              {{ vehicle.plateNumber }} / {{ vehicle.brand }}
            </option>
          </select>
          <select v-model.number="checkInForm.parkingLotId">
            <option v-for="lot in dataset.parkingLots" :key="lot.id" :value="lot.id">
              {{ lot.name }} / 空闲 {{ lot.freeSpaces }}
            </option>
          </select>
          <button type="submit">发起停车</button>
        </form>
      </article>

      <article v-if="ownerTab === 'vehicles'" class="panel owner-mobile-panel">
        <div class="panel-header">
          <h2>车辆管理</h2>
          <span>绑定和维护车主车辆</span>
        </div>
        <form class="compact-form owner-checkin-form" @submit.prevent="addVehicle">
          <select v-model.number="vehicleForm.userId">
            <option v-for="user in ownerUsers" :key="user.id" :value="user.id">
              {{ user.realName }}
            </option>
          </select>
          <input v-model="vehicleForm.plateNumber" placeholder="车牌号" />
          <input v-model="vehicleForm.brand" placeholder="品牌" />
          <input v-model="vehicleForm.color" placeholder="颜色" />
          <button type="submit">绑定车辆</button>
        </form>
        <div class="vehicle-grid">
          <div v-for="vehicle in ownerVehicles" :key="vehicle.id" class="vehicle-card">
            <strong>{{ vehicle.plateNumber }}</strong>
            <span>{{ vehicle.brand }} / {{ vehicle.color }}</span>
            <p>{{ vehicle.ownerName }}</p>
          </div>
        </div>
      </article>

      <article v-if="ownerTab === 'records'" class="panel owner-mobile-panel">
        <div class="panel-header">
          <h2>停车记录</h2>
          <span>查看在场记录与历史停车情况</span>
        </div>
        <div class="active-records">
          <div v-for="record in ownerRecords" :key="record.id" class="active-card">
            <div>
              <strong>{{ record.plateNumber }} / {{ record.parkingLotName }}</strong>
              <span>入场时间：{{ record.entryTime }}</span>
              <span v-if="record.exitTime">出场时间：{{ record.exitTime }}</span>
              <span v-if="record.status === '已完成'">已缴费用：{{ record.amount }} 元</span>
              <span v-else>当前状态：在场，待支付</span>
            </div>
            <button v-if="record.status === '在场'" class="ghost-button" @click="checkOutVehicle(record.id)">支付停车费用</button>
            <span v-else class="badge success">已完成</span>
          </div>
        </div>
      </article>

      <article v-if="ownerTab === 'orders'" class="panel owner-mobile-panel">
        <div class="panel-header">
          <h2>订单查看</h2>
          <span>查看停车缴费订单</span>
        </div>
        <div class="notice-list">
          <div v-for="order in ownerOrders" :key="order.id" class="notice-card">
            <div class="notice-title">
              <strong>{{ order.plateNumber }}</strong>
              <span class="badge success">{{ order.paymentStatus }}</span>
            </div>
            <p>{{ order.parkingLotName }}</p>
            <p>支付金额：{{ order.amount }} 元</p>
            <p>支付时间：{{ order.createdAt }}</p>
          </div>
        </div>
      </article>
    </section>

    <section v-else class="section">
      <article v-if="adminTab === 'income'" class="panel">
        <div class="panel-header">
          <h2>收入统计</h2>
          <span>聚焦经营指标与停车场收入情况</span>
        </div>
        <div class="card-grid metrics">
          <article v-for="metric in dataset.overview.metrics" :key="metric.title" class="metric-card">
            <span>{{ metric.title }}</span>
            <strong>{{ metric.value }}</strong>
            <p>{{ metric.description }}</p>
          </article>
        </div>
        <div class="dual-grid admin-summary-grid">
          <div v-for="item in adminRevenueSummary" :key="item.name" class="suggestion-card">
            <div>
              <h3>{{ item.name }}</h3>
              <span>订单 {{ item.orderCount }} 笔 / 占用率 {{ item.occupancyRate }}</span>
            </div>
            <p>累计收入：{{ item.income.toFixed(2) }} 元</p>
          </div>
        </div>
      </article>

      <div v-if="adminTab === 'records'" class="dual-grid">
        <article class="panel">
          <div class="panel-header">
            <h2>停车记录</h2>
            <span>查看车辆停车全过程</span>
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
                  <td><span class="badge" :class="record.status === '在场' ? 'warning' : 'success'">{{ record.status }}</span></td>
                  <td>
                    <button v-if="record.status === '在场'" class="ghost-button" @click="checkOutVehicle(record.id)">结算出场</button>
                    <span v-else>{{ record.amount }} 元</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>

        <article class="panel">
          <div class="panel-header">
            <h2>订单记录</h2>
            <span>对照停车订单与支付结果</span>
          </div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>车牌</th>
                  <th>停车场</th>
                  <th>支付金额</th>
                  <th>状态</th>
                  <th>支付时间</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="order in dataset.orders" :key="order.id">
                  <td>{{ order.plateNumber }}</td>
                  <td>{{ order.parkingLotName }}</td>
                  <td>{{ order.amount }} 元</td>
                  <td><span class="badge success">{{ order.paymentStatus }}</span></td>
                  <td>{{ order.createdAt }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>
      </div>

      <div v-if="adminTab === 'dashboard'" class="dual-grid">
        <article class="panel">
          <div class="panel-header">
            <h2>停车场数据筛选</h2>
            <span>可切换停车场和车位类别</span>
          </div>
          <form class="compact-form dashboard-filter-form">
            <select v-model.number="selectedLotId">
              <option v-for="lot in dataset.parkingLots" :key="lot.id" :value="lot.id">
                {{ lot.name }}
              </option>
            </select>
            <div class="category-switch">
              <button type="button" class="ghost-button" :class="{ 'active-switch': lotCategoryFilter === '全部' }" @click="lotCategoryFilter = '全部'">全部</button>
              <button type="button" class="ghost-button" :class="{ 'active-switch': lotCategoryFilter === '新能源' }" @click="lotCategoryFilter = '新能源'">新能源车</button>
              <button type="button" class="ghost-button" :class="{ 'active-switch': lotCategoryFilter === '普通' }" @click="lotCategoryFilter = '普通'">普通车</button>
            </div>
          </form>
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
            <h2>{{ selectedLot?.name ?? '停车场数据看板' }}</h2>
            <span>{{ lotCategoryFilter }}车位数据</span>
          </div>
          <div class="card-grid metrics lot-dashboard-grid">
            <article v-for="metric in lotDashboardMetrics" :key="metric.title" class="metric-card">
              <span>{{ metric.title }}</span>
              <strong>{{ metric.value }}</strong>
              <p>{{ metric.description }}</p>
            </article>
          </div>
          <div class="space-tags">
            <span v-for="space in lotDashboardSpaces" :key="space.id" class="tag" :class="space.status === '占用' ? 'danger' : 'normal'">
              {{ space.code }} / {{ space.category }} / {{ space.status }}
            </span>
          </div>
          <div class="log-list">
            <div v-for="record in lotDashboardRecords" :key="record.id" class="log-item">
              <strong>{{ record.plateNumber }} / {{ record.status }}</strong>
              <span>{{ record.parkingLotName }} · {{ record.spaceCode }}</span>
              <small>{{ record.entryTime }}</small>
            </div>
          </div>
        </article>
      </div>
    </section>
  </div>

  <div v-else class="loading-state">
    <strong>{{ loading ? '系统加载中...' : '暂无数据' }}</strong>
    <span>{{ banner }}</span>
  </div>
</template>
