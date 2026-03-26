import { createDemoDataset } from '../mock/demoData'
import type {
  AppDataset,
  LoginResult,
  LoginPayload,
  RegisterPayload,
  RecordUpdatePayload,
  ParkingLot,
  ParkingLotPayload,
  ParkingRecord,
  User,
  Vehicle,
  VehiclePayload,
} from '../types'

const API_BASE = 'http://localhost:8080/api'

class DemoRepository {
  private dataset = structuredClone(createDemoDataset())
  private credentials = new Map<string, string>([
    ['admin', '123456'],
    ['manager', '123456'],
    ['owner', '123456'],
  ])

  async getDataset(): Promise<AppDataset> {
    return structuredClone(this.dataset)
  }

  async login(payload: LoginPayload) {
    const user = this.dataset.users.find((item) => item.username === payload.username)
    if (!user || this.credentials.get(payload.username) !== payload.password) {
      throw new Error('账号或密码错误')
    }
    return {
      token: `demo-token-${payload.username}`,
      user,
    }
  }

  async registerOwner(payload: RegisterPayload): Promise<User> {
    const exists = this.dataset.users.some((item) => item.username === payload.username)
    if (exists) {
      throw new Error('用户名已存在')
    }
    const user = {
      id: this.dataset.users.length + 1,
      username: payload.username,
      realName: payload.realName,
      role: '车主',
      responsibility: '负责停车查询、车辆管理、停车缴费与订单查看',
      phone: payload.phone,
      status: '启用',
    }
    this.dataset.users.push(user)
    this.credentials.set(payload.username, payload.password)
    this.dataset.logs.unshift({
      id: this.dataset.logs.length + 1,
      action: '车主注册',
      detail: `${payload.realName} 完成注册，账号 ${payload.username}`,
      createdAt: formatDate(new Date()),
    })
    return user
  }

  async createVehicle(payload: VehiclePayload): Promise<Vehicle> {
    const owner = this.dataset.users.find((item) => item.id === payload.userId)
    if (!owner) {
      throw new Error('车主不存在')
    }
    const vehicle: Vehicle = {
      id: this.dataset.vehicles.length + 1,
      userId: payload.userId,
      ownerName: owner.realName,
      plateNumber: payload.plateNumber.toUpperCase(),
      brand: payload.brand,
      color: payload.color,
      status: '正常',
    }
    this.dataset.vehicles.unshift(vehicle)
    this.dataset.logs.unshift({
      id: this.dataset.logs.length + 1,
      action: '新增车辆档案',
      detail: `${owner.realName} 新增车辆 ${vehicle.plateNumber}`,
      createdAt: formatDate(new Date()),
    })
    return vehicle
  }

  async deleteVehicle(vehicleId: number) {
    const activeRecord = this.dataset.records.find((item) => item.vehicleId === vehicleId && item.status === '在场')
    if (activeRecord) {
      throw new Error('该车辆存在在场记录，不能删除')
    }
    const vehicle = this.dataset.vehicles.find((item) => item.id === vehicleId)
    if (!vehicle) {
      throw new Error('车辆不存在')
    }
    this.dataset.vehicles = this.dataset.vehicles.filter((item) => item.id !== vehicleId)
    this.dataset.logs.unshift({
      id: this.dataset.logs.length + 1,
      action: '删除车辆档案',
      detail: `${vehicle.ownerName} 删除车辆 ${vehicle.plateNumber}`,
      createdAt: formatDate(new Date()),
    })
    return true
  }

  async createParkingLot(payload: ParkingLotPayload): Promise<ParkingLot> {
    const parkingLot: ParkingLot = {
      id: this.dataset.parkingLots.length + 1,
      name: payload.name,
      code: payload.code,
      address: payload.address,
      managerName: '系统新增',
      businessHours: payload.businessHours,
      totalSpaces: payload.totalSpaces,
      occupiedSpaces: 0,
      freeSpaces: payload.totalSpaces,
      status: '运营中',
    }
    this.dataset.parkingLots.unshift(parkingLot)
    this.dataset.pricingRules.unshift({
      id: parkingLot.id,
      parkingLotId: parkingLot.id,
      parkingLotName: parkingLot.name,
      baseMinutes: 30,
      baseFee: 0,
      hourlyFee: 4,
      dailyCap: 38,
    })
    this.dataset.logs.unshift({
      id: this.dataset.logs.length + 1,
      action: '新增停车场',
      detail: `${parkingLot.name} 已加入系统，车位数 ${parkingLot.totalSpaces}`,
      createdAt: formatDate(new Date()),
    })
    return parkingLot
  }

  async checkIn(vehicleId: number, parkingLotId: number): Promise<ParkingRecord> {
    const vehicle = this.dataset.vehicles.find((item) => item.id === vehicleId)
    const parkingLot = this.dataset.parkingLots.find((item) => item.id === parkingLotId)
    if (!vehicle || !parkingLot) {
      throw new Error('车辆或停车场不存在')
    }
    const alreadyInside = this.dataset.records.find((item) => item.vehicleId === vehicleId && item.status === '在场')
    if (alreadyInside) {
      throw new Error('该车辆已在场内，不能重复入场')
    }
    parkingLot.occupiedSpaces += 1
    parkingLot.freeSpaces -= 1
    const record: ParkingRecord = {
      id: this.dataset.records.length + 1,
      vehicleId,
      plateNumber: vehicle.plateNumber,
      ownerName: vehicle.ownerName,
      parkingLotId,
      parkingLotName: parkingLot.name,
      spaceCode: `${parkingLot.code}-AUTO-${String(this.dataset.records.length + 1).padStart(3, '0')}`,
      entryTime: formatDate(new Date()),
      exitTime: null,
      durationMinutes: 0,
      amount: 0,
      status: '在场',
    }
    this.dataset.records.unshift(record)
    this.dataset.logs.unshift({
      id: this.dataset.logs.length + 1,
      action: '车辆入场',
      detail: `${vehicle.plateNumber} 进入 ${parkingLot.name}`,
      createdAt: formatDate(new Date()),
    })
    return record
  }

  async checkOut(recordId: number) {
    const record = this.dataset.records.find((item) => item.id === recordId)
    if (!record || record.status !== '在场') {
      throw new Error('当前停车记录无法出场')
    }
    const exitTime = new Date()
    const entryTime = new Date(record.entryTime.replace(' ', 'T'))
    const durationMinutes = Math.max(30, Math.round((exitTime.getTime() - entryTime.getTime()) / 60000))
    const parkingLot = this.dataset.parkingLots.find((item) => item.id === record.parkingLotId)
    if (parkingLot) {
      parkingLot.occupiedSpaces = Math.max(0, parkingLot.occupiedSpaces - 1)
      parkingLot.freeSpaces += 1
    }
    record.exitTime = formatDate(exitTime)
    record.durationMinutes = durationMinutes
    record.amount = Math.min(Math.ceil(durationMinutes / 60) * 4, 40)
    record.status = '已完成'
    this.dataset.orders.unshift({
      id: this.dataset.orders.length + 1,
      recordId: record.id,
      plateNumber: record.plateNumber,
      parkingLotName: record.parkingLotName,
      amount: record.amount,
      paymentStatus: '未支付',
      createdAt: record.exitTime,
    })
    this.dataset.logs.unshift({
      id: this.dataset.logs.length + 1,
      action: '车辆出场待支付',
      detail: `${record.plateNumber} 离开 ${record.parkingLotName}，待支付 ${record.amount} 元`,
      createdAt: formatDate(new Date()),
    })
    return structuredClone(record)
  }

  async payOrder(recordId: number) {
    const order = this.dataset.orders.find((item) => item.recordId === recordId)
    if (!order) {
      throw new Error('订单不存在')
    }
    if (order.paymentStatus === '已支付') {
      return structuredClone(order)
    }
    order.paymentStatus = '已支付'
    order.createdAt = formatDate(new Date())
    this.dataset.logs.unshift({
      id: this.dataset.logs.length + 1,
      action: '支付停车订单',
      detail: `${order.plateNumber} 完成停车支付 ${order.amount} 元`,
      createdAt: formatDate(new Date()),
    })
    return structuredClone(order)
  }

  async updateRecord(recordId: number, payload: RecordUpdatePayload) {
    const record = this.dataset.records.find((item) => item.id === recordId)
    if (!record) {
      throw new Error('停车记录不存在')
    }

    record.entryTime = payload.entryTime
    record.exitTime = payload.exitTime || null
    record.amount = Number(payload.amount)

    if (record.exitTime) {
      const entryTime = new Date(record.entryTime.replace(' ', 'T'))
      const exitTime = new Date(record.exitTime.replace(' ', 'T'))
      record.durationMinutes = Math.max(0, Math.round((exitTime.getTime() - entryTime.getTime()) / 60000))
      record.status = '已完成'

      const currentOrder = this.dataset.orders.find((item) => item.recordId === recordId)
      if (currentOrder) {
        currentOrder.amount = record.amount
        currentOrder.paymentStatus = payload.paymentStatus
        currentOrder.createdAt = record.exitTime
      } else {
        this.dataset.orders.unshift({
          id: this.dataset.orders.length + 1,
          recordId,
          plateNumber: record.plateNumber,
          parkingLotName: record.parkingLotName,
          amount: record.amount,
          paymentStatus: payload.paymentStatus,
          createdAt: record.exitTime,
        })
      }
    } else {
      record.durationMinutes = 0
      record.amount = 0
      record.status = '在场'
      this.dataset.orders = this.dataset.orders.filter((item) => item.recordId !== recordId)
    }

    this.dataset.logs.unshift({
      id: this.dataset.logs.length + 1,
      action: '编辑停车记录',
      detail: `${record.plateNumber} 的停车记录已更新`,
      createdAt: formatDate(new Date()),
    })
    return structuredClone(record)
  }
}

const demoRepository = new DemoRepository()

async function safeFetch<T>(path: string, init?: RequestInit): Promise<T | null> {
  try {
    const response = await fetch(`${API_BASE}${path}`, {
      headers: {
        'Content-Type': 'application/json',
      },
      ...init,
    })
    if (!response.ok) {
      return null
    }
    const payload = await response.json()
    return payload.data as T
  } catch {
    return null
  }
}

export const api = {
  async getDataset(): Promise<AppDataset> {
    const responses = await Promise.all([
      safeFetch('/auth/me'),
      safeFetch('/dashboard/overview'),
      safeFetch('/dashboard/dispatch-suggestions'),
      safeFetch('/parking/lots'),
      safeFetch('/parking/spaces'),
      safeFetch('/parking/pricing-rules'),
      safeFetch('/users'),
      safeFetch('/users/vehicles'),
      safeFetch('/records'),
      safeFetch('/records/orders'),
      safeFetch('/system/notices'),
      safeFetch('/system/logs'),
    ])

    const allAvailable = responses.every((item) => item !== null)
    if (allAvailable) {
      return {
        projectInfo: responses[0] as AppDataset['projectInfo'],
        overview: responses[1] as AppDataset['overview'],
        dispatchSuggestions: responses[2] as AppDataset['dispatchSuggestions'],
        parkingLots: responses[3] as AppDataset['parkingLots'],
        spaces: responses[4] as AppDataset['spaces'],
        pricingRules: responses[5] as AppDataset['pricingRules'],
        users: responses[6] as AppDataset['users'],
        vehicles: responses[7] as AppDataset['vehicles'],
        records: responses[8] as AppDataset['records'],
        orders: responses[9] as AppDataset['orders'],
        notices: responses[10] as AppDataset['notices'],
        logs: responses[11] as AppDataset['logs'],
      }
    }
    return demoRepository.getDataset()
  },

  async login(payload: LoginPayload) {
    const result = await safeFetch<LoginResult>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    return result ?? demoRepository.login(payload)
  },

  async registerOwner(payload: RegisterPayload): Promise<User> {
    const result = await safeFetch<User>('/auth/register', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    return result ?? demoRepository.registerOwner(payload)
  },

  async createVehicle(payload: VehiclePayload) {
    const result = await safeFetch('/users/vehicles', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    return result ?? demoRepository.createVehicle(payload)
  },

  async deleteVehicle(vehicleId: number) {
    const result = await safeFetch(`/users/vehicles/${vehicleId}`, {
      method: 'DELETE',
    })
    return result ?? demoRepository.deleteVehicle(vehicleId)
  },

  async createParkingLot(payload: ParkingLotPayload) {
    const result = await safeFetch('/parking/lots', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    return result ?? demoRepository.createParkingLot(payload)
  },

  async checkIn(vehicleId: number, parkingLotId: number) {
    const result = await safeFetch('/records/check-in', {
      method: 'POST',
      body: JSON.stringify({ vehicleId, parkingLotId }),
    })
    return result ?? demoRepository.checkIn(vehicleId, parkingLotId)
  },

  async checkOut(recordId: number) {
    const result = await safeFetch('/records/check-out', {
      method: 'POST',
      body: JSON.stringify({ recordId }),
    })
    return result ?? demoRepository.checkOut(recordId)
  },

  async payOrder(recordId: number) {
    const result = await safeFetch('/records/orders/pay', {
      method: 'POST',
      body: JSON.stringify({ recordId }),
    })
    return result ?? demoRepository.payOrder(recordId)
  },

  async updateRecord(recordId: number, payload: RecordUpdatePayload) {
    const result = await safeFetch(`/records/${recordId}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
    return result ?? demoRepository.updateRecord(recordId, payload)
  },
}

function formatDate(date: Date) {
  const parts = [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0'),
  ]
  const time = [
    String(date.getHours()).padStart(2, '0'),
    String(date.getMinutes()).padStart(2, '0'),
    String(date.getSeconds()).padStart(2, '0'),
  ]
  return `${parts.join('-')} ${time.join(':')}`
}
