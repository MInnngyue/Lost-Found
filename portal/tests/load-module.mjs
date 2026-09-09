import { readFile } from 'node:fs/promises'
import { SourceTextModule, SyntheticModule } from 'node:vm'

export async function loadModule(relativePath, mocks = {}) {
  const cache = new Map()
  async function load(url) {
    if (cache.has(url.href)) return cache.get(url.href)
    const mod = new SourceTextModule(await readFile(url, 'utf8'), { identifier: url.href })
    cache.set(url.href, mod)
    await mod.link(async (specifier, parent) => {
      if (Object.hasOwn(mocks, specifier)) {
        const exports = mocks[specifier]
        return new SyntheticModule(Object.keys(exports), function () {
          for (const [key, value] of Object.entries(exports)) this.setExport(key, value)
        })
      }
      const target = specifier.startsWith('@/')
        ? new URL(`../src/${specifier.slice(2)}`, import.meta.url)
        : new URL(specifier, parent.identifier)
      if (!target.pathname.endsWith('.js')) target.pathname += '.js'
      return load(target)
    })
    return mod
  }
  const mod = await load(new URL(`../src/${relativePath}`, import.meta.url))
  await mod.evaluate()
  return mod.namespace
}
