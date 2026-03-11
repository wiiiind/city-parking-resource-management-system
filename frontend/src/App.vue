<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from './services/api'
import type { AppDataset, ParkingLotPayload, User, VehiclePayload } from './types'

const viewConfig = {
  dashboard: '数据看板',
  admin: '管理端',
  user: '用户端',
} as const

const views = Object.keys(viewConfig) as Array<keyof typeof viewConfig>

const dataset = ref<AppDataset | null>(null)
const loading = ref(true)
const banner = ref('正在加载城市停车资源管理系统演示数据...')
const selectedView = ref<'dashboard' | 'admin' | 'user'>('dashboard')
const currentAccount = ref('')

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

const ownerUsers = computed(() => dataset.value?.users.filter((item) => item.role === '车主') ?? [])
const currentUser = computed<User | undefined>(() =>
  dataset.value?.users.find((item) => item.username === currentAccount.value),
)
const ownerVehicles = computed(() =>
  dataset.value?.vehicles.filter((item) => item.userId === currentUser.value?.id) ?? [],
)
const ownerRecords = computed(() =>
  dataset.value?.records.filter((item) => item.ownerName === currentUser.value?.realName) ?? [],
)
const ownerOrders = computed(() =>
  dataset.value?.orders.filter((item) => item.plateNumber && ownerVehicles.value.some((vehicle) => vehicle.plateNumber === item.plateNumber)) ?? [],
)
const lotPricingMap = computed(() => {
  const entries: Array<[number, string]> =
    dataset.value?.pricingRules.map((rule) => [
      rule.parkingLotId,
      `首${rule.baseMinutes}分钟${rule.baseFee}元，之后${rule.hourlyFee}元/小时，封顶${rule.dailyCap}元`,
    ]) ?? []
  return new Map(entries)
})
const loggedIn = computed(() => Boolean(currentUser.value))
const isOwnerMobileView = computed(() => currentUser.value?.role === '车主')
const availableViews = computed(() => {
  if (!currentUser.value) {
    return [] as (typeof views)
  }

  if (currentUser.value.role === '车主') {
    return ['user'] as (typeof views)
  }

  if (currentUser.value.role === '停车场管理员') {
    return ['admin'] as (typeof views)
  }

  return views
})
const showViewTabs = computed(() => availableViews.value.length > 1)

async function refresh() {
  dataset.value = await api.getDataset()
}

async function loadPage() {
  loading.value = true
  await refresh()
  banner.value = '系统已就绪。若本地后端未启动，页面将自动使用内置演示数据。'
  loading.value = false
}

function quickLogin(username: string) {
  const user = dataset.value?.users.find((item) => item.username === username)
  if (!user) {
    banner.value = '未找到演示账号'
    return
  }
  currentAccount.value = user.username
  selectedView.value = user.role === '车主' ? 'user' : user.role === '停车场管理员' ? 'admin' : 'dashboard'
  banner.value = `当前演示身份已切换为：${user.realName}（${user.role}）`
}

function logout() {
  currentAccount.value = ''
  selectedView.value = 'dashboard'
  banner.value = '已退出登录，请重新选择演示身份。'
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
      userId: currentUser.value?.id ?? 3,
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
  <div v-if="dataset && !loggedIn" class="login-shell">
    <section class="login-panel">
      <div class="login-copy">
        <span class="eyebrow">City Parking Resource Management</span>
        <h1>城市停车资源管理系统</h1>
        <p>
          请选择要进入的演示身份。系统会根据身份自动切换为桌面端管理界面或车主移动端，无需连接数据库校验密码。
        </p>
        <div class="login-tips">
          <span>管理员：进入数据看板与综合管理</span>
          <span>停车场管理员：直接进入资源管理页面</span>
          <span>车主：直接进入移动端车主页面</span>
        </div>
      </div>
      <div class="login-cards">
        <button
          v-for="user in dataset.users"
          :key="user.id"
          class="login-card"
          @click="quickLogin(user.username)"
        >
          <span>{{ user.role }}</span>
          <strong>{{ user.realName }}</strong>
          <p>{{ user.responsibility }}</p>
          <small>点击进入 {{ user.role === '车主' ? '移动端' : '工作台' }}</small>
        </button>
      </div>
    </section>
  </div>

  <div v-else-if="dataset" class="page-shell" :class="{ 'owner-mobile-shell': isOwnerMobileView }">
    <header class="hero">
      <div class="hero-copy">
        <span class="eyebrow">{{ isOwnerMobileView ? 'Owner Mobile Mode' : 'Graduation Project 2026' }}</span>
        <h1>{{ isOwnerMobileView ? '车主移动端' : dataset.projectInfo.projectName }}</h1>
        <p>
          {{
            isOwnerMobileView
              ? '为车主提供停车场查询、车辆绑定、停车记录查看与停车费用缴纳，页面按移动端答辩展示方式布局。'
              : '围绕“城市停车资源管理”完成停车资源维护、车辆进出场、订单结算、可视化看板、调度建议与材料整理，适合作为毕设演示与文档交付的一体化作品。'
          }}
        </p>
        <div class="hero-actions">
          <template v-if="showViewTabs">
            <button
              v-for="view in availableViews"
              :key="view"
              class="pill"
              :class="{ active: selectedView === view }"
              @click="setView(view)"
            >
              {{ viewConfig[view] }}
            </button>
          </template>
          <button class="pill logout-pill" @click="logout">退出登录</button>
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

    <section v-if="selectedView === 'dashboard' && !isOwnerMobileView" class="section">
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

    <section v-if="selectedView === 'admin' && !isOwnerMobileView" class="section">
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
      <div class="dual-grid" :class="{ 'owner-mobile-grid': isOwnerMobileView }">
        <article class="panel">
          <div class="panel-header">
            <h2>当前登录身份</h2>
            <span>进入系统后自动跳转到对应角色界面</span>
          </div>
          <div class="account-cards">
            <div class="account-card current-account-card">
              <strong>{{ currentUser?.realName }}</strong>
              <span>{{ currentUser?.role }}</span>
              <p>{{ currentUser?.responsibility }}</p>
              <small>如需切换角色，请先退出登录回到首页选择身份。</small>
            </div>
          </div>
        </article>

        <article class="panel">
          <div class="panel-header">
            <h2>查找停车场</h2>
            <span>突出车主查询与选择能力</span>
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

        <article class="panel">
          <div class="panel-header">
            <h2>绑定车辆与发起停车</h2>
            <span>保留车主端必要操作</span>
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
              <option v-for="vehicle in ownerVehicles" :key="vehicle.id" :value="vehicle.id">
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
            <div v-for="vehicle in ownerVehicles" :key="vehicle.id" class="vehicle-card">
              <strong>{{ vehicle.plateNumber }}</strong>
              <span>{{ vehicle.brand }} / {{ vehicle.color }}</span>
              <p>{{ vehicle.ownerName }}</p>
            </div>
          </div>
        </article>
      </div>

      <div class="dual-grid" :class="{ 'owner-mobile-grid': isOwnerMobileView }">
        <article class="panel" :class="{ 'owner-mobile-panel': isOwnerMobileView }">
          <div class="panel-header">
            <h2>我的停车记录</h2>
            <span>突出查询与缴费闭环</span>
          </div>
          <div class="active-records">
            <div v-for="record in ownerRecords" :key="record.id" class="active-card">
              <div>
                <strong>{{ record.plateNumber }} / {{ record.parkingLotName }}</strong>
                <span>入场时间：{{ record.entryTime }}</span>
                <span v-if="record.status === '已完成'">已缴费用：{{ record.amount }} 元</span>
                <span v-else>当前状态：在场，待出场结算</span>
              </div>
              <button v-if="record.status === '在场'" class="ghost-button" @click="checkOutVehicle(record.id)">支付停车费用</button>
              <span v-else class="badge success">已完成</span>
            </div>
          </div>
        </article>

        <article class="panel" :class="{ 'owner-mobile-panel': isOwnerMobileView }">
          <div class="panel-header">
            <h2>我的缴费订单</h2>
            <span>展示费用结算结果</span>
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
      </div>
    </section>

  </div>

  <div v-else class="loading-state">
    <strong>{{ loading ? '系统加载中...' : '暂无数据' }}</strong>
    <span>{{ banner }}</span>
  </div>
</template>
