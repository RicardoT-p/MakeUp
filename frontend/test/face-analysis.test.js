import test from 'node:test'
import assert from 'node:assert/strict'
import { sourceDimensions, stabilityOf } from '../src/face-analysis.js'
import { cameraErrorMessage } from '../src/camera.js'

test('stable readings pass after three samples', () => {
  const samples = [1, 1.01, .99].map(faceLengthWidth => ({ faceLengthWidth, noseWidthRatio: .25 }))
  assert.equal(stabilityOf(samples).stable, true)
})

test('large variation asks for another capture', () => {
  const samples = [1, 1.4, .7].map(faceLengthWidth => ({ faceLengthWidth, noseWidthRatio: .25 }))
  assert.equal(stabilityOf(samples).stable, false)
})

test('camera failures report the real cause', () => {
  assert.match(cameraErrorMessage({ name: 'NotReadableError' }, true), /占用/)
  assert.match(cameraErrorMessage({ name: 'NotAllowedError' }, true), /权限/)
  assert.match(cameraErrorMessage(null, false), /HTTPS/)
})

test('canvas dimensions are accepted for captured photos', () => {
  assert.deepEqual(sourceDimensions({ width: 1280, height: 720 }), { width: 1280, height: 720 })
})
