import { useMemo } from "react"

export type PostCategoryName = "ENGINEERING" | "PRODUCT" | "DESIGN" | "ETC" | "UNDEFINED"

export type PostCategory = {
  id: number
  name: PostCategoryName
  title: string
}

const UNDEFINED_CATEGORY: PostCategory = {
  id: 999,
  name: "UNDEFINED",
  title: "",
}

const VALID_CATEGORIES: PostCategory[] = [
  { id: 10, name: "ENGINEERING", title: "엔지니어링" },
  { id: 20, name: "PRODUCT", title: "프로덕트" },
  { id: 30, name: "DESIGN", title: "디자인" },
  { id: 40, name: "ETC", title: "기타" },
]

const CATEGORY_BY_ID = new Map<number, PostCategory>(VALID_CATEGORIES.map((category) => [category.id, category]))
const CATEGORY_BY_NAME = new Map<string, PostCategory>(VALID_CATEGORIES.map((category) => [category.name, category]))

export const postCategory = {
  validCategories: VALID_CATEGORIES,
  undefined: UNDEFINED_CATEGORY,
  fromId: (id?: number | null): PostCategory | null => {
    if (typeof id !== "number") return null
    return CATEGORY_BY_ID.get(id) ?? null
  },
  fromName: (name: string): PostCategory => {
    const category = CATEGORY_BY_NAME.get(name)
    if (!category) {
      throw new Error("존재하지 않는 카테고리입니다.")
    }
    return category
  },
}

export const usePostCategory = (categoryId?: number | null): PostCategory => {
  return useMemo(() => postCategory.fromId(categoryId) ?? postCategory.undefined, [categoryId])
}

export default usePostCategory
