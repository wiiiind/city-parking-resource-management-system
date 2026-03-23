<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from './services/api'
import type { AppDataset, LoginPayload, Order, ParkingLotPayload, RegisterPayload, User, VehiclePayload } from './types'

const dataset = ref<AppDataset | null>(null)
const loading = ref(true)
const banner = ref('正在加载城市停车资源管理系统演示数据...')
const currentAccount = ref('')
const adminLoginMode = ref(false)
const ownerRegisterMode = ref(false)
const ownerTab = ref<'home' | 'query' | 'vehicles' | 'payment' | 'orders'>('home')
const adminTab = ref<'income' | 'records' | 'dashboard' | 'createLot'>('income')
const lotCategoryFilter = ref<'全部' | '新能源' | '普通'>('全部')
const selectedLotId = ref(1)
const adminRecordKeyword = ref('')

const loginForm = reactive<LoginPayload>({
  username: 'owner',
  password: '123456',
})

const registerForm = reactive<RegisterPayload>({
  username: '',
  password: '',
  realName: '',
  phone: '',
})

const adminLoginForm = reactive<LoginPayload>({
  username: 'admin',
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

const ownerNav = [
  { key: 'home', label: '功能首页' },
  { key: 'query', label: '停车查询' },
  { key: 'vehicles', label: '车辆管理' },
  { key: 'payment', label: '停车缴费' },
  { key: 'orders', label: '订单查看' },
] as const

const adminNav = [
  { key: 'income', label: '收入统计' },
  { key: 'records', label: '停车记录订单记录' },
  { key: 'dashboard', label: '单停车场看板' },
  { key: 'createLot', label: '新增停车场' },
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
const ownerPendingPayments = computed(() => ownerRecords.value.filter((item) => item.status === '在场'))
const ownerOrderRows = computed(() =>
  ownerRecords.value.map((record) => {
    const order = orderMap.value.get(record.id)
    return {
      recordId: record.id,
      plateNumber: record.plateNumber,
      parkingLotName: record.parkingLotName,
      amount: order?.amount ?? record.amount,
      paymentStatus: order?.paymentStatus ?? '未支付',
      exitTime: record.exitTime,
      createdAt: order?.createdAt ?? '-',
    }
  }),
)
const orderRecordMap = computed(() => {
  const entries: Array<[number, string | null]> =
    dataset.value?.records.map((record) => [record.id, record.exitTime]) ?? []
  return new Map(entries)
})
const orderMap = computed(() => {
  const entries: Array<[number, Order]> = dataset.value?.orders.map((order) => [order.recordId, order]) ?? []
  return new Map(entries)
})
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
const occupiedVehicleRows = computed(() =>
  lotDashboardRecords.value
    .filter((record) => record.status === '在场')
    .map((record) => ({
      plateNumber: record.plateNumber,
      ownerName: record.ownerName,
      spaceCode: record.spaceCode,
      entryTime: record.entryTime,
    })),
)
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
const adminMergedRecords = computed(() => {
  if (!dataset.value) {
    return []
  }
  const keyword = adminRecordKeyword.value.trim().toLowerCase()
  return dataset.value.records.filter((record) => {
    const matchKeyword =
      !keyword ||
      record.plateNumber.toLowerCase().includes(keyword) ||
      record.parkingLotName.toLowerCase().includes(keyword)
    return matchKeyword
  })
})
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
    adminLoginMode.value = false
    ownerTab.value = 'home'
    banner.value = `已登录车主端：${user.realName}`
  } catch (error) {
    banner.value = error instanceof Error ? error.message : '车主登录失败'
  }
}

async function submitOwnerRegister() {
  try {
    const user = await api.registerOwner(registerForm)
    await refresh()
    loginForm.username = user.username
    loginForm.password = registerForm.password
    Object.assign(registerForm, {
      username: '',
      password: '',
      realName: '',
      phone: '',
    })
    ownerRegisterMode.value = false
    banner.value = '注册成功，请使用新账号登录。'
  } catch (error) {
    banner.value = error instanceof Error ? error.message : '注册失败'
  }
}

function enterAdminPortal() {
  adminLoginMode.value = true
  banner.value = '请输入管理员账号进入后台。'
}

async function submitAdminLogin() {
  const user = dataset.value?.users.find((item) => item.username === 'admin')
  try {
    await api.login(adminLoginForm)
    if (!user || adminLoginForm.username !== 'admin') {
      throw new Error('请使用管理员账号登录')
    }
    currentAccount.value = user.username
    adminLoginMode.value = false
    adminTab.value = 'income'
    banner.value = `已进入管理员后台：${user.realName}`
  } catch (error) {
    banner.value = error instanceof Error ? error.message : '管理员登录失败'
  }
}

function logout() {
  currentAccount.value = ''
  adminLoginMode.value = false
  loginForm.username = 'owner'
  loginForm.password = '123456'
  ownerRegisterMode.value = false
  adminLoginForm.username = 'admin'
  adminLoginForm.password = '123456'
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

async function removeVehicle(vehicleId: number) {
  try {
    await api.deleteVehicle(vehicleId)
    await refresh()
    banner.value = '车辆删除成功。'
  } catch (error) {
    banner.value = error instanceof Error ? error.message : '删除车辆失败'
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

function deleteRecord(recordId: number) {
  if (!dataset.value) {
    return
  }
  dataset.value.records = dataset.value.records.filter((record) => record.id !== recordId)
  dataset.value.orders = dataset.value.orders.filter((order) => order.recordId !== recordId)
  banner.value = `记录 ${recordId} 已删除。`
}

function editRecord(recordId: number) {
  banner.value = `记录 ${recordId} 已进入编辑模式（答辩演示版）。`
}

onMounted(loadPage)
</script>

<template>
  <div v-if="dataset && !loggedIn" class="login-shell">
    <section class="login-panel login-panel-split">
      <div class="login-copy">
        <span class="eyebrow">City Parking Resource Management</span>
        <h1>城市停车资源管理系统</h1>
        <div class="brand-mark">
          <div class="brand-icon">
            <span>P</span>
          </div>
          <div>
            <strong>City Parking</strong>
            <p>统一停车查询、订单查看与经营分析入口</p>
          </div>
        </div>
      </div>

      <div class="login-stack">
        <article v-if="!adminLoginMode" class="login-card login-form-card">
          <span>{{ ownerRegisterMode ? '车主注册' : '车主登录' }}</span>
          <strong>{{ ownerRegisterMode ? '注册车主账号' : '进入停车服务' }}</strong>
          <form v-if="!ownerRegisterMode" class="stack-form" @submit.prevent="submitOwnerLogin">
            <input v-model="loginForm.username" placeholder="车主账号" />
            <input v-model="loginForm.password" type="password" placeholder="登录密码" />
            <button type="submit">登录车主端</button>
          </form>
          <form v-else class="stack-form" @submit.prevent="submitOwnerRegister">
            <input v-model="registerForm.username" placeholder="注册账号" />
            <input v-model="registerForm.realName" placeholder="姓名" />
            <input v-model="registerForm.phone" placeholder="手机号" />
            <input v-model="registerForm.password" type="password" placeholder="设置密码" />
            <button type="submit">注册车主</button>
          </form>
          <button class="ghost-button inline-ghost" @click="ownerRegisterMode = !ownerRegisterMode">
            {{ ownerRegisterMode ? '返回登录' : '没有账号？去注册' }}
          </button>
        </article>

        <article v-else class="login-card login-form-card admin-login-card">
          <span>系统管理员</span>
          <strong>管理员登录</strong>
          <form class="stack-form" @submit.prevent="submitAdminLogin">
            <input v-model="adminLoginForm.username" placeholder="管理员账号" />
            <input v-model="adminLoginForm.password" type="password" placeholder="登录密码" />
            <button type="submit">登录后台</button>
          </form>
          <button class="ghost-button inline-ghost" @click="adminLoginMode = false">返回首页</button>
        </article>

        <button v-if="!adminLoginMode" class="login-card admin-entry-card" @click="enterAdminPortal">
          <span>系统管理员</span>
          <strong>进入后台管理</strong>
          <small>点击进入管理员登录页</small>
        </button>
      </div>
    </section>
  </div>

  <div v-else-if="dataset" class="page-shell" :class="{ 'owner-mobile-shell': isOwnerMobileView }">
    <header class="hero">
      <div class="hero-copy">
        <span class="eyebrow">{{ isOwnerMobileView ? 'Owner Service Portal' : 'Admin Control Center' }}</span>
        <h1>{{ isOwnerMobileView ? '车主服务端' : '系统管理员后台' }}</h1>
        <span class="hero-user-chip">{{ currentUser?.realName }} / {{ currentUser?.role }}</span>
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
    </header>

    <section v-if="isOwnerMobileView" class="section">
      <article v-if="ownerTab === 'home'" class="panel owner-mobile-panel">
        <div class="panel-header">
          <h2>车主功能入口</h2>
        </div>
        <div class="entry-grid">
          <button class="entry-card" @click="ownerTab = 'query'">
            <strong>停车查询</strong>
            <span>查看停车场与收费规则</span>
          </button>
          <button class="entry-card" @click="ownerTab = 'vehicles'">
            <strong>车辆管理</strong>
            <span>新增和维护车辆信息</span>
          </button>
          <button class="entry-card" @click="ownerTab = 'payment'">
            <strong>停车缴费</strong>
            <span>处理在场车辆的缴费</span>
          </button>
          <button class="entry-card" @click="ownerTab = 'orders'">
            <strong>订单查看</strong>
            <span>查看已支付和未支付订单</span>
          </button>
        </div>
      </article>

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
            <button class="ghost-button inline-ghost" @click="removeVehicle(vehicle.id)">删除车辆</button>
          </div>
        </div>
      </article>

      <article v-if="ownerTab === 'payment'" class="panel owner-mobile-panel">
        <div class="panel-header">
          <h2>停车缴费</h2>
          <span>只保留待支付停车记录</span>
        </div>
        <div class="active-records">
          <div v-for="record in ownerPendingPayments" :key="record.id" class="active-card">
            <div>
              <strong>{{ record.plateNumber }} / {{ record.parkingLotName }}</strong>
              <span>入场时间：{{ record.entryTime }}</span>
              <span>当前状态：在场，待支付</span>
            </div>
            <button class="ghost-button" @click="checkOutVehicle(record.id)">支付停车费用</button>
          </div>
        </div>
      </article>

      <article v-if="ownerTab === 'orders'" class="panel owner-mobile-panel">
        <div class="panel-header">
          <h2>订单查看</h2>
          <span>查看已支付和未支付订单</span>
        </div>
        <div class="notice-list">
          <div v-for="order in ownerOrderRows" :key="order.recordId" class="notice-card">
            <div class="notice-title">
              <strong>{{ order.plateNumber }}</strong>
              <span class="badge" :class="order.paymentStatus === '已支付' ? 'success' : 'warning'">{{ order.paymentStatus }}</span>
            </div>
            <p>{{ order.parkingLotName }}</p>
            <p>支付金额：{{ order.amount }} 元</p>
            <p>出场时间：{{ orderRecordMap.get(order.recordId) ?? '待出场' }}</p>
            <p>支付时间：{{ order.createdAt }}</p>
          </div>
        </div>
      </article>
    </section>

    <section v-else class="section">
      <article v-if="adminTab === 'income'" class="panel">
        <div class="panel-header">
          <h2>收入统计</h2>
        </div>
        <div class="card-grid metrics">
          <article v-for="metric in dataset.overview.metrics" :key="metric.title" class="metric-card">
            <span>{{ metric.title }}</span>
            <strong>{{ metric.value }}</strong>
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

      <article v-if="adminTab === 'records'" class="panel">
        <div class="panel-header">
          <h2>停车记录与订单记录</h2>
        </div>
        <form class="compact-form dashboard-filter-form admin-search-form" @submit.prevent>
          <input v-model="adminRecordKeyword" placeholder="按车牌号或停车场检索" />
        </form>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>车牌</th>
                <th>停车场</th>
                <th>入场时间</th>
                <th>出场时间</th>
                <th>费用</th>
                <th>订单状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="record in adminMergedRecords" :key="record.id">
                <td>{{ record.plateNumber }}</td>
                <td>{{ record.parkingLotName }}</td>
                <td>{{ record.entryTime }}</td>
                <td>{{ record.exitTime ?? '待出场' }}</td>
                <td>{{ orderMap.get(record.id)?.amount ?? record.amount }} 元</td>
                <td>
                  <span class="badge" :class="record.status === '在场' ? 'warning' : 'success'">
                    {{ orderMap.get(record.id)?.paymentStatus ?? record.status }}
                  </span>
                </td>
                <td class="action-cell">
                  <button v-if="record.status === '在场'" class="ghost-button" @click="checkOutVehicle(record.id)">结算</button>
                  <button class="ghost-button" @click="editRecord(record.id)">编辑</button>
                  <button class="ghost-button danger-ghost" @click="deleteRecord(record.id)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <div v-if="adminTab === 'dashboard'" class="dual-grid">
        <article class="panel">
          <div class="panel-header">
            <h2>停车场数据筛选</h2>
          </div>
          <div class="dashboard-left-layout">
            <div class="lot-selector-card">
              <span>选择停车场</span>
              <select v-model.number="selectedLotId">
                <option v-for="lot in dataset.parkingLots" :key="lot.id" :value="lot.id">
                  {{ lot.name }}
                </option>
              </select>
            </div>
            <div class="dashboard-mini-grid">
              <button type="button" class="mini-filter-card" :class="{ 'active-switch': lotCategoryFilter === '全部' }" @click="lotCategoryFilter = '全部'">
                <strong>全部</strong>
                <span>查看全部车位</span>
              </button>
              <button type="button" class="mini-filter-card" :class="{ 'active-switch': lotCategoryFilter === '新能源' }" @click="lotCategoryFilter = '新能源'">
                <strong>新能源车</strong>
                <span>筛选新能源车位</span>
              </button>
              <button type="button" class="mini-filter-card" :class="{ 'active-switch': lotCategoryFilter === '普通' }" @click="lotCategoryFilter = '普通'">
                <strong>普通车</strong>
                <span>筛选普通车位</span>
              </button>
            </div>
          </div>
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
            </article>
          </div>
          <div class="space-tags">
            <span v-for="space in lotDashboardSpaces" :key="space.id" class="tag" :class="space.status === '占用' ? 'danger' : 'normal'">
              {{ space.code }} / {{ space.category }} / {{ space.status }}
            </span>
          </div>
          <div class="table-wrap occupied-table-wrap">
            <table>
              <thead>
                <tr>
                  <th>车牌</th>
                  <th>车主</th>
                  <th>车位</th>
                  <th>入场时间</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in occupiedVehicleRows" :key="item.plateNumber + item.spaceCode">
                  <td>{{ item.plateNumber }}</td>
                  <td>{{ item.ownerName }}</td>
                  <td>{{ item.spaceCode }}</td>
                  <td>{{ item.entryTime }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>
      </div>

      <article v-if="adminTab === 'createLot'" class="panel">
        <div class="panel-header">
          <h2>新增停车场</h2>
        </div>
        <form class="compact-form create-lot-form" @submit.prevent="addParkingLot">
          <input v-model="parkingLotForm.name" placeholder="新增停车场名称" />
          <input v-model="parkingLotForm.code" placeholder="停车场编码" />
          <input v-model="parkingLotForm.address" placeholder="停车场地址" />
          <input v-model.number="parkingLotForm.totalSpaces" type="number" min="1" placeholder="总车位数" />
          <input v-model="parkingLotForm.businessHours" placeholder="营业时间" />
          <button type="submit">新增停车场</button>
        </form>
      </article>
    </section>
  </div>

  <div v-else class="loading-state">
    <strong>{{ loading ? '系统加载中...' : '暂无数据' }}</strong>
    <span>{{ banner }}</span>
  </div>
</template>
