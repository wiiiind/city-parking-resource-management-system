<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { api } from './services/api'
import type { AppDataset, LoginPayload, Order, ParkingLotPayload, RecordUpdatePayload, RegisterPayload, User, VehiclePayload } from './types'

const dataset = ref<AppDataset | null>(null)
const loading = ref(true)
const banner = ref('正在加载城市停车资源管理系统演示数据...')
const currentAccount = ref('')
const adminLoginMode = ref(false)
const ownerRegisterMode = ref(false)
const ownerTab = ref<'home' | 'parking' | 'payment' | 'profile'>('home')
const ownerProfilePage = ref<'menu' | 'vehicles' | 'orders' | 'orderDetail'>('menu')
const vehicleFormVisible = ref(false)
const swipedVehicleId = ref<number | null>(null)
const vehicleSwipeStartX = ref(0)
const vehicleSwipeId = ref<number | null>(null)
const adminTab = ref<'income' | 'records' | 'dashboard' | 'createLot'>('income')
const lotCategoryFilter = ref<'全部' | '新能源' | '普通'>('全部')
const ownerParkingFilter = ref<'全部' | '空位优先' | '全天开放'>('全部')
const parkingSearchKeyword = ref('')
const selectedParkingCity = ref('')
const parkingCityMenuOpen = ref(false)
const parkingSearchActive = ref(false)
const selectedLotId = ref(1)
const adminRecordKeyword = ref('')
const editingRecordId = ref<number | null>(null)
const selectedOwnerOrderId = ref<number | null>(null)

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

const recordEditForm = reactive<RecordUpdatePayload>({
  entryTime: '',
  exitTime: '',
  amount: 0,
  paymentStatus: '已支付',
})

const ownerNav = [
  { key: 'home', label: '首页' },
  { key: 'parking', label: '停车' },
  { key: 'payment', label: '缴费' },
  { key: 'profile', label: '我的' },
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
const ownerLatestOrder = computed(() => ownerOrderRows.value[0] ?? null)
const selectedOwnerOrder = computed(() =>
  ownerOrderRows.value.find((order) => order.recordId === selectedOwnerOrderId.value) ?? null,
)
const ownerParkingOverview = computed(() => {
  const rows = ownerPendingPayments.value.map((record) => ({
    id: record.id,
    plateNumber: record.plateNumber,
    parkingLotName: record.parkingLotName,
    durationText: formatDurationMinutes(record.durationMinutes),
    amountText: `${record.amount}元`,
  }))
  const totalAmount = ownerPendingPayments.value.reduce((sum, record) => sum + Number(record.amount), 0)
  return {
    count: rows.length,
    totalAmount: `${totalAmount}元`,
    items: rows.slice(0, 2),
    hiddenCount: Math.max(0, rows.length - 2),
  }
})
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
const ownerParkingLots = computed(() => {
  if (!dataset.value) {
    return []
  }

  const keyword = parkingSearchKeyword.value.trim().toLowerCase()
  let list = dataset.value.parkingLots.filter((lot) => {
    const matchCity = !selectedParkingCity.value || lot.address.includes(selectedParkingCity.value)
    if (!keyword) {
      return matchCity
    }
    return matchCity && (lot.name.toLowerCase().includes(keyword) || lot.address.toLowerCase().includes(keyword))
  })

  if (ownerParkingFilter.value === '全天开放') {
    list = list.filter((lot) => lot.businessHours.includes('全天'))
  }

  if (ownerParkingFilter.value === '空位优先') {
    list = [...list].sort((a, b) => b.freeSpaces - a.freeSpaces)
  }

  return list
})
const ownerParkingCities = computed(() => {
  if (!dataset.value) {
    return []
  }
  const cities = new Set(
    dataset.value.parkingLots
      .map((lot) => {
        const match = lot.address.match(/^(.+?市)/)
        return match?.[1]
      }),
  )
  return Array.from(cities).filter((city): city is string => Boolean(city))
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
  const cities = Array.from(
    new Set(
      (dataset.value?.parkingLots ?? [])
        .map((lot) => lot.address.match(/^(.+?市)/)?.[1])
        .filter((city): city is string => Boolean(city)),
    ),
  )
  if (cities.length > 0 && (!selectedParkingCity.value || !cities.includes(selectedParkingCity.value))) {
    selectedParkingCity.value = cities[0]!
  }
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
    ownerProfilePage.value = 'menu'
    vehicleFormVisible.value = false
    swipedVehicleId.value = null
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
  ownerProfilePage.value = 'menu'
  selectedOwnerOrderId.value = null
  vehicleFormVisible.value = false
  swipedVehicleId.value = null
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
    vehicleForm.userId = currentUser.value?.id ?? 3
    await api.createVehicle(vehicleForm)
    await refresh()
    Object.assign(vehicleForm, {
      userId: currentUser.value?.id ?? 3,
      plateNumber: '',
      brand: '',
      color: '',
    })
    vehicleFormVisible.value = false
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
    ownerTab.value = 'profile'
    ownerProfilePage.value = 'orderDetail'
    selectedOwnerOrderId.value = recordId
    banner.value = '已生成待支付订单，请确认支付。'
  } catch (error) {
    banner.value = error instanceof Error ? error.message : '车辆出场失败'
  }
}

async function payOwnerOrder(recordId: number) {
  try {
    await api.payOrder(recordId)
    await refresh()
    ownerTab.value = 'profile'
    ownerProfilePage.value = 'orderDetail'
    selectedOwnerOrderId.value = recordId
    banner.value = '支付成功，订单状态已更新。'
  } catch (error) {
    banner.value = error instanceof Error ? error.message : '支付失败'
  }
}

function selectOwnerTab(tab: 'home' | 'parking' | 'payment' | 'profile') {
  ownerTab.value = tab
  if (tab === 'profile') {
    ownerProfilePage.value = 'menu'
    selectedOwnerOrderId.value = null
  }
}

function openOwnerProfilePage(page: 'vehicles' | 'orders') {
  ownerTab.value = 'profile'
  ownerProfilePage.value = page
  if (page === 'vehicles') {
    swipedVehicleId.value = null
  }
  if (page !== 'orders') {
    selectedOwnerOrderId.value = null
  }
}

function openOwnerOrderDetail(recordId: number) {
  selectedOwnerOrderId.value = recordId
  ownerProfilePage.value = 'orderDetail'
}

function backFromOwnerSubpage() {
  if (ownerProfilePage.value === 'orderDetail') {
    ownerProfilePage.value = 'orders'
    return
  }
  ownerProfilePage.value = 'menu'
}

function onVehicleSwipeStart(vehicleId: number, event: TouchEvent | MouseEvent) {
  vehicleSwipeId.value = vehicleId
  swipedVehicleId.value = swipedVehicleId.value === vehicleId ? vehicleId : null
  if ('touches' in event) {
    vehicleSwipeStartX.value = event.touches[0]?.clientX ?? 0
    return
  }
  vehicleSwipeStartX.value = event.clientX
}

function onVehicleSwipeEnd(vehicleId: number, event: TouchEvent | MouseEvent) {
  if (vehicleSwipeId.value !== vehicleId) {
    return
  }
  let endX = 0
  if ('changedTouches' in event) {
    endX = event.changedTouches[0]?.clientX ?? vehicleSwipeStartX.value
  } else {
    endX = event.clientX
  }
  const deltaX = endX - vehicleSwipeStartX.value
  if (deltaX < -38) {
    swipedVehicleId.value = vehicleId
  } else if (deltaX > 24) {
    swipedVehicleId.value = null
  }
  vehicleSwipeId.value = null
}

function formatDurationMinutes(minutes: number) {
  const safeMinutes = Math.max(0, Math.round(minutes))
  const hours = Math.floor(safeMinutes / 60)
  const remainingMinutes = safeMinutes % 60
  return hours > 0 ? `${hours}小时${remainingMinutes}分钟` : `${remainingMinutes}分钟`
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
  const record = dataset.value?.records.find((item) => item.id === recordId)
  if (!record) {
    banner.value = '停车记录不存在。'
    return
  }
  const currentOrder = orderMap.value.get(recordId)
  editingRecordId.value = recordId
  recordEditForm.entryTime = record.entryTime
  recordEditForm.exitTime = record.exitTime ?? ''
  recordEditForm.amount = currentOrder?.amount ?? record.amount
  recordEditForm.paymentStatus = currentOrder?.paymentStatus ?? '已支付'
  banner.value = `正在编辑记录 ${recordId}。`
}

function cancelRecordEdit() {
  editingRecordId.value = null
  recordEditForm.entryTime = ''
  recordEditForm.exitTime = ''
  recordEditForm.amount = 0
  recordEditForm.paymentStatus = '已支付'
}

async function saveRecordEdit() {
  if (editingRecordId.value === null) {
    return
  }
  try {
    await api.updateRecord(editingRecordId.value, {
      entryTime: recordEditForm.entryTime,
      exitTime: recordEditForm.exitTime,
      amount: Number(recordEditForm.amount),
      paymentStatus: recordEditForm.paymentStatus,
    })
    await refresh()
    banner.value = `记录 ${editingRecordId.value} 已更新。`
    cancelRecordEdit()
  } catch (error) {
    banner.value = error instanceof Error ? error.message : '停车记录更新失败'
  }
}

function ownerPageTitle() {
  if (ownerTab.value === 'profile') {
    if (ownerProfilePage.value === 'vehicles') return '车辆信息'
    if (ownerProfilePage.value === 'orders') return '我的订单'
    if (ownerProfilePage.value === 'orderDetail') return '订单详情'
    return '我的'
  }
  return ownerTab.value === 'home' ? '城市停车' : ownerTab.value === 'parking' ? '停车查询' : '停车缴费'
}

onMounted(loadPage)

watch(ownerTab, async (tab) => {
  if (tab === 'parking') {
    await refresh()
  }
})

watch(selectedParkingCity, () => {
  parkingCityMenuOpen.value = false
})

function exitParkingSearch() {
  parkingSearchActive.value = false
  parkingSearchKeyword.value = ''
}
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

  <div v-else-if="dataset && isOwnerMobileView" class="page-shell owner-mobile-shell">
    <div class="mobile-app-frame">
      <header
        v-if="ownerTab !== 'profile' || ownerProfilePage !== 'menu'"
        class="mobile-header mobile-header-floating"
      >
        <div class="mobile-header-floating-side">
          <button
            v-if="ownerTab === 'profile' && ownerProfilePage !== 'menu'"
            class="ghost-button owner-floating-back-button"
            @click="backFromOwnerSubpage"
          >
            ‹
          </button>
        </div>
        <div class="mobile-header-floating-center">
          <div class="mobile-header-floating-bar">
            <h1>{{ ownerPageTitle() }}</h1>
          </div>
        </div>
        <div class="mobile-header-floating-side mobile-header-floating-side-end">
          <button
            v-if="ownerTab === 'profile' && ownerProfilePage === 'vehicles'"
            class="ghost-button owner-floating-back-button owner-floating-add-button"
            @click="vehicleFormVisible = !vehicleFormVisible"
          >
            ＋
          </button>
        </div>
      </header>

      <div v-if="ownerTab === 'parking'" class="parking-search-floating">
        <div class="parking-search-bar" :class="{ 'parking-search-bar-active': parkingSearchActive }">
          <div class="parking-city-dropdown" :class="{ 'parking-search-hidden': parkingSearchActive }">
            <button class="parking-city-trigger" @click="parkingCityMenuOpen = !parkingCityMenuOpen">
              <span>{{ selectedParkingCity || '选择城市' }}</span>
            </button>
            <div v-if="parkingCityMenuOpen" class="parking-city-menu">
              <button
                v-for="city in ownerParkingCities"
                :key="city"
                class="parking-city-option"
                :class="{ active: selectedParkingCity === city }"
                @click="selectedParkingCity = city"
              >
                {{ city }}
              </button>
            </div>
          </div>
          <div class="parking-search-input-shell">
            <input
              v-model="parkingSearchKeyword"
              placeholder=""
              @focus="parkingSearchActive = true"
              @keydown.enter.prevent
            />
          </div>
          <button class="owner-primary-button parking-search-button" :class="{ 'parking-search-hidden': parkingSearchActive }">搜索</button>
          <button class="parking-search-close" :class="{ 'parking-search-close-visible': parkingSearchActive }" @click="exitParkingSearch">✕</button>
        </div>
      </div>

      <Transition name="vehicle-sheet">
        <div
          v-if="ownerTab === 'profile' && ownerProfilePage === 'vehicles' && vehicleFormVisible"
          class="vehicle-sheet-overlay"
        >
          <div class="vehicle-sheet-backdrop" @click="vehicleFormVisible = false" />
          <section class="vehicle-sheet-panel">
            <header class="vehicle-sheet-header">
              <button
                type="button"
                class="ghost-button owner-floating-back-button vehicle-sheet-header-button"
                @click="vehicleFormVisible = false"
              >
                ‹
              </button>
              <h2>车辆信息</h2>
              <button
                type="submit"
                form="vehicle-sheet-form"
                class="ghost-button owner-floating-back-button vehicle-sheet-header-button vehicle-sheet-confirm-button"
              >
                ✓
              </button>
            </header>
            <form
              id="vehicle-sheet-form"
              class="compact-form owner-checkin-form vehicle-inline-form vehicle-sheet-form"
              @submit.prevent="addVehicle"
            >
              <input v-model="vehicleForm.plateNumber" placeholder="车牌号" />
              <input v-model="vehicleForm.brand" placeholder="品牌" />
              <input v-model="vehicleForm.color" placeholder="颜色" />
            </form>
          </section>
        </div>
      </Transition>

      <section :class="['mobile-content', { 'mobile-content-with-header': ownerTab !== 'profile' || ownerProfilePage !== 'menu', 'mobile-content-with-parking-search': ownerTab === 'parking' }]">
        <article v-if="ownerTab === 'home'" class="owner-home-page">
          <section class="owner-hero-card">
            <div class="owner-hero-illustration">P</div>
            <template v-if="ownerParkingOverview.count > 0">
              <span class="owner-hero-eyebrow">当前停车概览</span>
              <strong>当前有 {{ ownerParkingOverview.count }} 辆车在场</strong>
              <div class="owner-hero-live-grid">
                <div class="owner-hero-live-card">
                  <span>待处理车辆</span>
                  <strong>{{ ownerParkingOverview.count }} 辆</strong>
                </div>
                <div class="owner-hero-live-card">
                  <span>当前总费用</span>
                  <strong>{{ ownerParkingOverview.totalAmount }}</strong>
                </div>
              </div>
              <div class="owner-hero-parking-list">
                <div v-for="item in ownerParkingOverview.items" :key="item.id" class="owner-hero-parking-item">
                  <div>
                    <strong>{{ item.plateNumber }}</strong>
                    <p>{{ item.parkingLotName }}</p>
                  </div>
                  <div class="owner-hero-parking-meta">
                    <span>{{ item.durationText }}</span>
                    <strong>{{ item.amountText }}</strong>
                  </div>
                </div>
                <p v-if="ownerParkingOverview.hiddenCount > 0" class="owner-hero-more-text">
                  还有 {{ ownerParkingOverview.hiddenCount }} 辆车辆在场
                </p>
              </div>
              <button class="owner-primary-button" @click="ownerTab = 'payment'">去处理待缴费</button>
            </template>
            <template v-else>
              <span class="owner-hero-eyebrow">车主服务</span>
              <strong>{{ ownerVehicles.length > 0 ? '当前没有车辆在场' : '添加车牌，缴费更快捷' }}</strong>
              <p>{{ ownerVehicles.length > 0 ? '可直接查看停车场、历史订单和待缴记录。' : '支持停车查询、待缴费处理和订单查看，减少排队等待。' }}</p>
              <button class="owner-primary-button" @click="ownerVehicles.length > 0 ? ownerTab = 'parking' : openOwnerProfilePage('vehicles')">
                {{ ownerVehicles.length > 0 ? '去停车查询' : '去添加车辆' }}
              </button>
            </template>
            <div class="owner-quick-grid">
              <button class="owner-quick-item" @click="ownerTab = 'parking'">
                <span>位</span>
                <strong>找车位</strong>
              </button>
              <button class="owner-quick-item" @click="openOwnerProfilePage('vehicles')">
                <span>车</span>
                <strong>我的车辆</strong>
              </button>
              <button class="owner-quick-item" @click="openOwnerProfilePage('orders')">
                <span>单</span>
                <strong>查订单</strong>
              </button>
              <button class="owner-quick-item" @click="ownerTab = 'payment'">
                <span>缴</span>
                <strong>停车缴费</strong>
              </button>
            </div>
          </section>

          <section class="owner-message-strip">
            <strong>最近消息</strong>
            <span>{{ ownerLatestOrder ? `${ownerLatestOrder.plateNumber} 最近一笔订单 ${ownerLatestOrder.paymentStatus}` : '登录后查看最新停车消息' }}</span>
          </section>

          <section class="owner-benefit-grid">
            <article class="owner-benefit-card owner-benefit-card-large">
              <span>已绑定车辆</span>
              <strong>{{ ownerVehicles.length }}</strong>
              <p>已录入系统的车辆信息</p>
            </article>
            <article class="owner-benefit-card">
              <span>待处理缴费</span>
              <strong>{{ ownerPendingPayments.length }}</strong>
              <p>可在“缴费”中立即处理</p>
            </article>
            <article class="owner-benefit-card">
              <span>最近订单</span>
              <strong>{{ ownerLatestOrder?.amount ?? 0 }} 元</strong>
              <p>{{ ownerLatestOrder?.parkingLotName ?? '暂无订单记录' }}</p>
            </article>
          </section>
        </article>

      <article v-if="ownerTab === 'parking'" class="panel owner-mobile-panel">
        <div class="parking-filter-row">
          <button
            class="parking-filter-chip"
            :class="{ active: ownerParkingFilter === '全部' }"
            @click="ownerParkingFilter = '全部'"
          >
            全部
          </button>
          <button
            class="parking-filter-chip"
            :class="{ active: ownerParkingFilter === '空位优先' }"
            @click="ownerParkingFilter = '空位优先'"
          >
            空位优先
          </button>
          <button
            class="parking-filter-chip"
            :class="{ active: ownerParkingFilter === '全天开放' }"
            @click="ownerParkingFilter = '全天开放'"
          >
            全天开放
          </button>
        </div>

        <div class="parking-lot-list">
          <div v-for="lot in ownerParkingLots" :key="lot.id" class="parking-lot-card">
            <div class="parking-lot-top">
              <div class="parking-lot-thumb">{{ lot.name.slice(0, 2) }}</div>
              <div class="parking-lot-headline">
                <strong>{{ lot.name }}</strong>
                <span class="parking-status-chip">空闲 {{ lot.freeSpaces }}/{{ lot.totalSpaces }}</span>
              </div>
            </div>
            <div class="parking-lot-bottom">
              <p class="parking-lot-address">{{ lot.address }}</p>
              <p class="parking-lot-meta">营业时间：{{ lot.businessHours }}</p>
              <p class="parking-lot-meta">收费规则：{{ lotPricingMap.get(lot.id) ?? '按停车场规则计费' }}</p>
            </div>
          </div>
          <div v-if="ownerParkingLots.length === 0" class="notice-card">
            <strong>暂无匹配停车场</strong>
            <p>请更换关键词或筛选条件后重试。</p>
          </div>
        </div>
      </article>

      <article v-if="ownerTab === 'payment'" class="panel owner-mobile-panel">
        <div class="panel-header">
          <span>只保留待支付停车记录</span>
        </div>
        <div class="active-records">
          <div v-for="record in ownerPendingPayments" :key="record.id" class="payment-record-card">
            <div class="payment-record-top">
              <div class="payment-record-status">待缴</div>
              <div class="payment-record-main">
                <strong>{{ record.plateNumber }}</strong>
                <span>{{ record.parkingLotName }}</span>
              </div>
              <div class="payment-record-amount">
                <strong>{{ record.amount }}元</strong>
              </div>
            </div>
            <div class="payment-record-bottom">
              <p>入场时间：{{ record.entryTime }}</p>
              <p>当前状态：在场</p>
            </div>
            <button class="ghost-button payment-record-action" @click="checkOutVehicle(record.id)">结束停车并生成订单</button>
          </div>
        </div>
      </article>

        <article v-if="ownerTab === 'profile'" class="owner-profile-page">
          <section v-if="ownerProfilePage === 'menu'" class="owner-profile-hero">
            <div class="owner-avatar">车</div>
            <div>
              <strong>{{ currentUser?.realName }}</strong>
              <p>{{ currentUser?.phone }}</p>
            </div>
          </section>

          <section v-if="ownerProfilePage === 'menu'" class="owner-profile-stats">
            <div>
              <strong>{{ ownerVehicles.length }}</strong>
              <span>我的车辆</span>
            </div>
            <div>
              <strong>{{ ownerOrderRows.length }}</strong>
              <span>停车订单</span>
            </div>
            <div>
              <strong>{{ ownerPendingPayments.length }}</strong>
              <span>待缴记录</span>
            </div>
          </section>

          <section v-if="ownerProfilePage === 'menu'" class="owner-group-card">
            <h3>交易管理</h3>
            <div class="owner-tool-grid">
              <button class="owner-tool-item" @click="openOwnerProfilePage('vehicles')">
                <span>车</span>
                <strong>车辆信息</strong>
              </button>
              <button class="owner-tool-item" @click="openOwnerProfilePage('orders')">
                <span>缴</span>
                <strong>我的订单</strong>
              </button>
            </div>
          </section>

          <section v-if="ownerProfilePage === 'vehicles'" class="owner-group-card owner-group-card-plain">
            <div class="vehicle-overview vehicle-overview-full">
              <div
                v-for="vehicle in ownerVehicles"
                :key="vehicle.id"
                class="vehicle-swipe-row"
                :class="{ 'vehicle-swipe-row-open': swipedVehicleId === vehicle.id }"
              >
                <button class="vehicle-delete-reveal" @click="removeVehicle(vehicle.id)">🗑</button>
                <div
                  class="owner-detail-card owner-detail-card-vehicle vehicle-swipe-card"
                  @touchstart.passive="onVehicleSwipeStart(vehicle.id, $event)"
                  @touchend="onVehicleSwipeEnd(vehicle.id, $event)"
                  @mousedown="onVehicleSwipeStart(vehicle.id, $event)"
                  @mouseup="onVehicleSwipeEnd(vehicle.id, $event)"
                  @mouseleave="onVehicleSwipeEnd(vehicle.id, $event)"
                  @click="swipedVehicleId = swipedVehicleId === vehicle.id ? null : swipedVehicleId"
                >
                  <div class="owner-detail-thumb">{{ vehicle.plateNumber.slice(0, 1) }}</div>
                  <div class="owner-detail-main">
                    <div class="owner-detail-title-row">
                      <strong>{{ vehicle.plateNumber }}</strong>
                      <span class="parking-distance-text">{{ vehicle.status }}</span>
                    </div>
                    <p>{{ vehicle.brand }} / {{ vehicle.color }}</p>
                  </div>
                </div>
              </div>
            </div>
          </section>

          <section v-if="ownerProfilePage === 'orders'" class="owner-group-card owner-group-card-plain">
            <div class="notice-list vehicle-overview-full">
              <div v-for="order in ownerOrderRows" :key="order.recordId" class="owner-detail-card owner-detail-card-vehicle">
                <div class="owner-detail-thumb">单</div>
                <div class="owner-detail-main">
                  <div class="owner-detail-title-row">
                    <strong>{{ order.plateNumber }}</strong>
                    <span class="badge" :class="order.paymentStatus === '已支付' ? 'success' : 'warning'">{{ order.paymentStatus }}</span>
                  </div>
                  <p>{{ order.parkingLotName }}</p>
                  <button class="ghost-button inline-ghost" @click="openOwnerOrderDetail(order.recordId)">查看详情</button>
                </div>
              </div>
            </div>
          </section>

          <section v-if="ownerProfilePage === 'orderDetail' && selectedOwnerOrder" class="owner-group-card">
            <div class="owner-order-detail-card">
              <div class="owner-detail-title-row">
                <strong>{{ selectedOwnerOrder.plateNumber }}</strong>
                <span class="badge" :class="selectedOwnerOrder.paymentStatus === '已支付' ? 'success' : 'warning'">
                  {{ selectedOwnerOrder.paymentStatus }}
                </span>
              </div>
              <p>车牌号：{{ selectedOwnerOrder.plateNumber }}</p>
              <p>停车场：{{ selectedOwnerOrder.parkingLotName }}</p>
              <p>支付金额：{{ selectedOwnerOrder.amount }} 元</p>
              <p>出场时间：{{ orderRecordMap.get(selectedOwnerOrder.recordId) ?? '待出场' }}</p>
              <p>{{ selectedOwnerOrder.paymentStatus === '已支付' ? '支付时间' : '订单生成时间' }}：{{ selectedOwnerOrder.createdAt }}</p>
              <button
                v-if="selectedOwnerOrder.paymentStatus !== '已支付'"
                class="owner-primary-button"
                @click="payOwnerOrder(selectedOwnerOrder.recordId)"
              >
                确认支付
              </button>
            </div>
          </section>

          <button v-if="ownerProfilePage === 'menu'" class="ghost-button profile-logout-button" @click="logout">退出登录</button>
        </article>
      </section>

      <nav class="mobile-tabbar">
        <button
          v-for="item in ownerNav"
          :key="item.key"
          class="mobile-tab"
          :class="{ active: ownerTab === item.key }"
          @click="selectOwnerTab(item.key)"
        >
          <span class="mobile-tab-icon">{{ item.key === 'home' ? '首' : item.key === 'parking' ? '停' : item.key === 'payment' ? '缴' : '我' }}</span>
          <span>{{ item.label }}</span>
        </button>
      </nav>
    </div>
  </div>

  <div v-else-if="dataset" class="page-shell">
    <header class="hero">
      <div class="hero-copy">
        <span class="eyebrow">Admin Control Center</span>
        <h1>系统管理员后台</h1>
        <span class="hero-user-chip">{{ currentUser?.realName }} / {{ currentUser?.role }}</span>
        <div class="hero-actions">
          <button
            v-for="item in adminNav"
            :key="item.key"
            class="pill"
            :class="{ active: adminTab === item.key }"
            @click="adminTab = item.key"
          >
            {{ item.label }}
          </button>
          <button class="pill logout-pill" @click="logout">退出登录</button>
        </div>
      </div>
    </header>

    <section class="section">
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
        <form v-if="editingRecordId !== null" class="record-edit-panel compact-form" @submit.prevent="saveRecordEdit">
          <input v-model="recordEditForm.entryTime" placeholder="入场时间：2026-03-24 11:02:26" />
          <input v-model="recordEditForm.exitTime" placeholder="出场时间：为空表示仍在场" />
          <input v-model.number="recordEditForm.amount" type="number" min="0" step="0.01" placeholder="费用" />
          <select v-model="recordEditForm.paymentStatus">
            <option value="已支付">已支付</option>
            <option value="未支付">未支付</option>
          </select>
          <button type="submit">保存编辑</button>
          <button type="button" class="ghost-button danger-ghost" @click="cancelRecordEdit">取消编辑</button>
        </form>
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
